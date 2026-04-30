package io.keploy.samples.simplededup;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public final class SimpleJavaDedupApplication {
    private SimpleJavaDedupApplication() {
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/healthz", new HealthHandler());
        server.createContext("/grade", new GradeHandler());
        server.createContext("/shipping", new ShippingHandler());
        server.createContext("/inventory", new InventoryHandler());
        server.createContext("/invoice", new InvoiceHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("simple-java-dedup listening on " + port);
    }

    private static final class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                respond(exchange, 405, "{\"error\":\"method_not_allowed\"}");
                return;
            }
            respond(exchange, 200, "{\"status\":\"ok\"}");
        }
    }

    private static final class GradeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            int score = parseInt(query.get("score"), 0);
            String grade;
            String message;
            if (score >= 90) {
                grade = "A";
                message = "excellent";
            } else if (score >= 75) {
                grade = "B";
                message = "solid";
            } else if (score >= 50) {
                grade = "C";
                message = "practice";
            } else {
                grade = "F";
                message = "retry";
            }
            respond(exchange, 200, "{\"score\":" + score + ",\"grade\":\"" + grade + "\",\"message\":\"" + message + "\"}");
        }
    }

    private static final class ShippingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            String country = query.getOrDefault("country", "US");
            int total = parseInt(query.get("total"), 0);
            String tier;
            int etaDays;
            if (!"US".equalsIgnoreCase(country)) {
                tier = "international";
                etaDays = 9;
            } else if (total >= 100) {
                tier = "free";
                etaDays = 2;
            } else {
                tier = "standard";
                etaDays = 5;
            }
            respond(exchange, 200, "{\"country\":\"" + country.toUpperCase() + "\",\"total\":" + total + ",\"tier\":\"" + tier + "\",\"etaDays\":" + etaDays + "}");
        }
    }

    private static final class InventoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            String sku = query.getOrDefault("sku", "BOOK-1").toUpperCase();
            int quantity = parseInt(query.get("quantity"), 1);
            String lane;
            boolean reserved;
            if (sku.startsWith("BOOK") && quantity <= 5) {
                lane = "priority";
                reserved = true;
            } else if (sku.startsWith("PEN") && quantity <= 10) {
                lane = "standard";
                reserved = true;
            } else {
                lane = "manual-review";
                reserved = false;
            }
            respond(exchange, 200, "{\"sku\":\"" + sku + "\",\"quantity\":" + quantity + ",\"lane\":\"" + lane + "\",\"reserved\":" + reserved + "}");
        }
    }

    private static final class InvoiceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            String customer = query.getOrDefault("customer", "guest").toLowerCase();
            int subtotal = parseInt(query.get("subtotal"), 0);
            int discount;
            String segment;
            if ("vip".equals(customer) && subtotal >= 200) {
                discount = 40;
                segment = "vip-large";
            } else if ("vip".equals(customer)) {
                discount = 15;
                segment = "vip";
            } else if (subtotal >= 100) {
                discount = 10;
                segment = "standard-large";
            } else {
                discount = 0;
                segment = "standard";
            }
            int total = subtotal - discount;
            respond(exchange, 200, "{\"customer\":\"" + customer + "\",\"subtotal\":" + subtotal + ",\"discount\":" + discount + ",\"total\":" + total + ",\"segment\":\"" + segment + "\"}");
        }
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> query = new HashMap<String, String>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return query;
        }
        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            int equals = pair.indexOf('=');
            if (equals < 0) {
                query.put(decode(pair), "");
            } else {
                query.put(decode(pair.substring(0, equals)), decode(pair.substring(equals + 1)));
            }
        }
        return query;
    }

    private static int parseInt(String value, int fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (Exception ignored) {
            return value;
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream responseBody = exchange.getResponseBody();
        try {
            responseBody.write(bytes);
        } finally {
            responseBody.close();
        }
    }
}
