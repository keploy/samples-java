package io.keploy.samples.dropwizarddedup.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CatalogService {

    public Map<String, Object> catalog(String category, int limit) {
        Map<String, Object> response = map(
                "category", category,
                "limit", limit
        );
        response.put("items", selectItems(category, limit));
        response.put("source", category.equals("electronics") ? "warehouse-b" : "warehouse-a");
        return response;
    }

    public Map<String, Object> item(String sku) {
        if ("BK-1".equals(sku)) {
            return item("BK-1", "Clean Architecture", "books", "available", "32.50");
        }
        if ("EL-1".equals(sku)) {
            return item("EL-1", "Noise Cancelling Headphones", "electronics", "backorder", "199.99");
        }
        return null;
    }

    public Map<String, Object> search(String term, String sort) {
        Map<String, Object> response = map("term", term, "sort", sort);
        response.put("ranking", "price".equals(sort) ? "discount-first" : "relevance-first");
        response.put("hits", Arrays.asList(
                item("EL-1", "Noise Cancelling Headphones", "electronics", "backorder", "199.99"),
                item("BK-1", "Clean Architecture", "books", "available", "32.50")
        ));
        return response;
    }

    public Map<String, Object> order(String customer, String sku, int quantity, boolean priority) {
        Map<String, Object> response = map(
                "orderId", priority ? "ORD-PRIORITY" : "ORD-STANDARD",
                "customer", customer,
                "sku", sku,
                "quantity", quantity,
                "priority", priority,
                "route", priority ? "air" : "ground"
        );
        response.put("checks", Arrays.asList("inventory", "pricing", priority ? "expedite" : "standard"));
        return response;
    }

    public Map<String, Object> orderStatus(String orderId, boolean expand) {
        Map<String, Object> response = map(
                "orderId", orderId,
                "status", "packed",
                "expand", expand
        );
        if (expand) {
            response.put("audit", Arrays.asList("created", "paid", "packed"));
        }
        return response;
    }

    public Map<String, Object> updateOrder(String orderId, String status) {
        return map("orderId", orderId, "status", status, "updated", true);
    }

    public Map<String, Object> deleteOrder(String orderId) {
        return map("orderId", orderId, "deleted", true);
    }

    private List<Map<String, Object>> selectItems(String category, int limit) {
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        if ("electronics".equals(category)) {
            items.add(item("EL-1", "Noise Cancelling Headphones", "electronics", "backorder", "199.99"));
            items.add(item("EL-2", "USB-C Dock", "electronics", "available", "89.00"));
        } else {
            items.add(item("BK-1", "Clean Architecture", "books", "available", "32.50"));
            items.add(item("BK-2", "Effective Java", "books", "available", "45.00"));
        }
        return items.subList(0, Math.min(Math.max(limit, 0), items.size()));
    }

    private Map<String, Object> item(String sku, String name, String category, String status, String price) {
        return map("sku", sku, "name", name, "category", category, "status", status, "price", price);
    }

    public static Map<String, Object> map(Object... values) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        for (int i = 0; i < values.length; i += 2) {
            response.put(String.valueOf(values[i]), values[i + 1]);
        }
        return response;
    }
}
