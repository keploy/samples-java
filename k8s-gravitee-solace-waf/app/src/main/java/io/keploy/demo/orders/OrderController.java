package io.keploy.demo.orders;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@RestController
public class OrderController {
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private static final Set<String> CURRENCIES = Set.of("EUR", "USD", "GBP", "INR");
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final OrderRepository repository;
    private final RedisCacheClient redis;
    private final InventoryClient inventory;
    private final PaymentsClient payments;
    private final SolacePublisher publisher;
    private final ObjectMapper mapper;
    private final String topic;

    OrderController(OrderRepository repository,
                    RedisCacheClient redis,
                    InventoryClient inventory,
                    PaymentsClient payments,
                    SolacePublisher publisher,
                    ObjectMapper mapper,
                    @Value("${orders.topic}") String topic) {
        this.repository = repository;
        this.redis = redis;
        this.inventory = inventory;
        this.payments = payments;
        this.publisher = publisher;
        this.mapper = mapper;
        this.topic = topic;
    }

    @GetMapping("/health")
    Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @PostMapping("/orders")
    ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> req,
                                               @RequestHeader(value = "X-Gravitee-Transaction-Id", required = false) String gwTx,
                                               @RequestHeader(value = "X-Sales-Channel", required = false) String channel) throws Exception {
        List<String> errors = new ArrayList<>();
        String customerId = str(req.get("customerId"));
        String reference = str(req.get("reference"));
        String currency = Optional.ofNullable(str(req.get("currency"))).orElse("EUR");
        String note = Optional.ofNullable(str(req.get("note"))).orElse("standard");
        if (customerId == null || !customerId.matches("C-\\d{3,8}")) errors.add("customerId must look like C-1234");
        if (reference == null || reference.isBlank()) errors.add("reference is required");
        if (!CURRENCIES.contains(currency)) errors.add("currency must be one of " + new TreeSet<>(CURRENCIES));

        List<Map<String, Object>> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        Object rawItems = req.get("items");
        if (!(rawItems instanceof List<?> list) || list.isEmpty()) {
            errors.add("items must be a non-empty list");
        } else {
            int i = 0;
            for (Object o : list) {
                if (!(o instanceof Map<?, ?> m)) { errors.add("items[" + i + "] must be an object"); i++; continue; }
                String sku = str(m.get("sku"));
                Integer qty = m.get("quantity") instanceof Number n ? n.intValue() : null;
                BigDecimal price = m.get("unitPrice") instanceof Number n ? new BigDecimal(n.toString()) : null;
                if (sku == null || sku.isBlank()) errors.add("items[" + i + "].sku is required");
                if (qty == null || qty < 1 || qty > 100) errors.add("items[" + i + "].quantity must be 1..100");
                if (price == null || price.signum() <= 0) errors.add("items[" + i + "].unitPrice must be > 0");
                if (sku != null && qty != null && price != null && qty >= 1 && price.signum() > 0) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("sku", sku);
                    item.put("quantity", qty);
                    item.put("unitPrice", price.setScale(2, RoundingMode.HALF_UP));
                    items.add(item);
                    total = total.add(price.multiply(BigDecimal.valueOf(qty)));
                }
                i++;
            }
        }
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "validation_failed", "details", errors));
        }

        String orderId = "ORD-" + shortHash(customerId + "|" + reference);
        total = total.setScale(2, RoundingMode.HALF_UP);
        String ch = channel == null || channel.isBlank() ? "web" : channel;

        // 1. Redis idempotency check (SET NX EX)
        if (!redis.claimIdempotency(customerId, reference, orderId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "duplicate_order", "orderId", orderId, "source", "redis_idempotency"));
        }

        // 2. Outbound partner payments authorization THROUGH the ModSecurity/CRS WAF
        PaymentsClient.Result pay = payments.authorize(orderId, customerId, total, currency, note);
        if (pay.status() == 403) {
            redis.releaseIdempotency(customerId, reference);
            Map<String, Object> rej = new LinkedHashMap<>();
            rej.put("error", "payment_rejected_by_waf");
            rej.put("wafStatus", 403);
            rej.put("orderId", orderId);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(rej);
        }
        if (pay.status() / 100 != 2) {
            redis.releaseIdempotency(customerId, reference);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "payment_failed", "paymentStatus", pay.status()));
        }

        // 3. Persist order in PostgreSQL
        repository.insert(orderId, customerId, reference, items, total, currency, ch, pay.paymentId(), note);

        // 4. Outbound inventory reservation THROUGH Gravitee + 5. Solace OrderCreated publish
        // Run both so a multi-dependency regression surfaces diffs on both Gravitee/inventory and Solace.
        int inventoryStatus;
        String reservationId = null;
        String inventoryGwTx = null;
        try {
            InventoryClient.Result inv = inventory.reserve(orderId, items, gwTx);
            inventoryStatus = inv.status();
            reservationId = inv.reservationId();
            inventoryGwTx = inv.gatewayTxId();
        } catch (Exception e) {
            log.warn("inventory reserve failed for {}: {}", orderId, e.toString());
            inventoryStatus = 502;
        }

        Map<String, Object> event = OrderEvents.orderCreated(orderId, customerId, items, total, currency, ch, pay.paymentId());
        int brokerStatus;
        try {
            brokerStatus = publisher.publish(topic, event, gwTx);
        } catch (Exception e) {
            log.warn("solace publish failed for {}: {}", orderId, e.toString());
            brokerStatus = 502;
        }

        if (inventoryStatus / 100 != 2 || brokerStatus / 100 != 2) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "downstream_failed");
            err.put("inventoryStatus", inventoryStatus);
            err.put("brokerStatus", brokerStatus);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(err);
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("orderId", orderId);
        resp.put("status", "CREATED");
        resp.put("customerId", customerId);
        resp.put("reference", reference);
        resp.put("items", items);
        resp.put("totalAmount", total);
        resp.put("currency", currency);
        resp.put("channel", ch);
        resp.put("paymentId", pay.paymentId());
        resp.put("reservationId", reservationId);
        resp.put("note", note);
        resp.put("publishedTo", topic);
        resp.put("gatewayTransactionId", gwTx);
        if (inventoryGwTx != null) {
            resp.put("inventoryGatewayTransactionId", inventoryGwTx);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/orders/{id}")
    ResponseEntity<Map<String, Object>> get(@PathVariable String id) throws Exception {
        String cached = redis.getOrder(id);
        if (cached != null) {
            Map<String, Object> o = mapper.readValue(cached, MAP_TYPE);
            o.put("cache", "HIT");
            return ResponseEntity.ok(o);
        }
        Optional<Map<String, Object>> found = repository.findById(id);
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "not_found", "orderId", id));
        }
        Map<String, Object> order = found.get();
        redis.putOrder(id, mapper.writeValueAsString(order));
        Map<String, Object> resp = new LinkedHashMap<>(order);
        resp.put("cache", "MISS");
        return ResponseEntity.ok(resp);
    }

    /**
     * Cancels an order end-to-end over native Solace SMF: publishes OrderCancelled to orders/cancelled,
     * consumes the broker-pushed SMF delivery via JCSMP XMLMessageConsumer, updates Postgres, and evicts Redis.
     */
    @PostMapping("/orders/{id}/cancel")
    ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable String id,
                                                    @RequestBody(required = false) Map<String, Object> body,
                                                    @RequestHeader(value = "X-Gravitee-Transaction-Id", required = false) String gwTx) throws Exception {
        String reason = body != null && body.get("reason") != null ? body.get("reason").toString() : "customer_requested";
        String via = publisher.publishAndConsumeCancel(id, reason, gwTx);
        int updated = repository.cancel(id);
        redis.evictOrder(id);
        log.info("cancelled order {} via {} (reason={}, updated={})", id, via, reason, updated);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("orderId", id);
        resp.put("status", "CANCELLED");
        resp.put("reason", reason);
        resp.put("consumedVia", via);
        resp.put("updated", updated);
        return ResponseEntity.ok(resp);
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static String shortHash(String s) throws Exception {
        byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().withUpperCase().formatHex(d, 0, 5);
    }
}
