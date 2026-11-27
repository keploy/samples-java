import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tiny internal inventory service behind Gravitee (/inventory -> inventory-api:8081/inventory).
 * POST /inventory/reserve validates X-Api-Key and returns a deterministic reservationId.
 */
public final class InventoryServer {
    private static final String EXPECTED_API_KEY = System.getenv().getOrDefault("INVENTORY_API_KEY", "orders-internal-key-v1");
    private static final Pattern ORDER_ID_RE = Pattern.compile("\"orderId\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern WAREHOUSE_RE = Pattern.compile("\"warehouse\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SKU_RE = Pattern.compile("\"sku\"\\s*:\\s*\"([^\"]+)\"");

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8081"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", ex -> send(ex, 200, "{\"status\":\"UP\"}"));
        server.createContext("/inventory/reserve", InventoryServer::handleReserve);
        server.createContext("/reserve", InventoryServer::handleReserve);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("inventory-api listening on :" + port);
    }

    private static void handleReserve(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "{\"error\":\"method_not_allowed\"}");
            return;
        }
        String apiKey = ex.getRequestHeaders().getFirst("X-Api-Key");
        if (apiKey == null || !apiKey.equals(EXPECTED_API_KEY)) {
            send(ex, 401, "{\"error\":\"invalid_api_key\"}");
            return;
        }
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String orderId = find(ORDER_ID_RE, body, "UNKNOWN");
        String warehouse = find(WAREHOUSE_RE, body, "EU-WEST-1");
        int itemsCount = 0;
        Matcher m = SKU_RE.matcher(body);
        while (m.find()) itemsCount++;
        if (itemsCount == 0) itemsCount = 1;

        String resId = "RES-" + shortHash(orderId + "|" + warehouse + "|" + itemsCount);
        String gw = ex.getRequestHeaders().getFirst("X-Gateway");
        String gwTx = ex.getRequestHeaders().getFirst("X-Gravitee-Transaction-Id");
        String resp = String.format(
                "{\"reservationId\":\"%s\",\"orderId\":\"%s\",\"status\":\"RESERVED\",\"warehouse\":\"%s\",\"itemsReserved\":%d,\"gateway\":\"%s\"}",
                esc(resId), esc(orderId), esc(warehouse), itemsCount, esc(gw == null ? "direct" : gw)
        );
        System.out.printf("reserved %s for %s warehouse=%s items=%d gw=%s tx=%s%n",
                resId, orderId, warehouse, itemsCount, gw, gwTx);
        send(ex, 200, resp);
    }

    private static String find(Pattern p, String s, String def) {
        Matcher m = p.matcher(s);
        return m.find() ? m.group(1) : def;
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String shortHash(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().withUpperCase().formatHex(d, 0, 5);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void send(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
