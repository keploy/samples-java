package io.keploy.demo.orders;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
class OrderRepository {
    private static final TypeReference<List<Map<String, Object>>> ITEMS_TYPE = new TypeReference<>() {};
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    OrderRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    void insert(String orderId, String customerId, String reference, List<Map<String, Object>> items,
                BigDecimal totalAmount, String currency, String channel, String paymentId, String note) throws Exception {
        String itemsJson = mapper.writeValueAsString(items);
        jdbc.update(
                "INSERT INTO orders (order_id, status, customer_id, reference, items_json, total_amount, currency, channel, payment_id, note) " +
                        "VALUES (?, 'CREATED', ?, ?, ?, ?, ?, ?, ?, ?)",
                orderId, customerId, reference, itemsJson, totalAmount, currency, channel, paymentId, note
        );
    }

    Optional<Map<String, Object>> findById(String orderId) {
        List<Map<String, Object>> rows = jdbc.query(
                "SELECT order_id, status, customer_id, reference, items_json, total_amount, currency, channel, payment_id, note " +
                        "FROM orders WHERE order_id = ?",
                (rs, rowNum) -> {
                    Map<String, Object> o = new LinkedHashMap<>();
                    o.put("orderId", rs.getString("order_id"));
                    o.put("status", rs.getString("status"));
                    o.put("customerId", rs.getString("customer_id"));
                    o.put("reference", rs.getString("reference"));
                    try {
                        o.put("items", mapper.readValue(rs.getString("items_json"), ITEMS_TYPE));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    o.put("totalAmount", rs.getBigDecimal("total_amount"));
                    o.put("currency", rs.getString("currency"));
                    o.put("channel", rs.getString("channel"));
                    o.put("paymentId", rs.getString("payment_id"));
                    o.put("note", rs.getString("note"));
                    return o;
                },
                orderId
        );
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    int cancel(String orderId) {
        return jdbc.update("UPDATE orders SET status = 'CANCELLED' WHERE order_id = ?", orderId);
    }
}
