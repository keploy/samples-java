package io.keploy.demo.orders;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds the OrderCreated event published to Solace. */
final class OrderEvents {
    static final String EVENT_VERSION = "1";

    private OrderEvents() {}

    static Map<String, Object> orderCreated(String orderId, String customerId, List<Map<String, Object>> items,
                                            BigDecimal total, String currency, String channel, String paymentId) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("eventType", "OrderCreated");
        e.put("eventVersion", EVENT_VERSION);
        e.put("orderId", orderId);
        e.put("customerId", customerId);
        e.put("items", items);
        e.put("totalAmount", total);
        e.put("currency", currency);
        e.put("channel", channel);
        e.put("paymentId", paymentId);
        return e;
    }
}
