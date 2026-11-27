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
 * Tiny partner payments service fronted by ModSecurity + OWASP CRS WAF (Host: payments.partner.local).
 * POST /payments/authorize returns a deterministic paymentId for requests allowed through the WAF.
 */
public final class PaymentsServer {
    private static final Pattern ORDER_ID_RE = Pattern.compile("\"orderId\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern CUSTOMER_RE = Pattern.compile("\"customerId\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern CURRENCY_RE = Pattern.compile("\"currency\"\\s*:\\s*\"([^\"]+)\"");

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8082"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", ex -> send(ex, 200, "{\"status\":\"UP\"}"));
        server.createContext("/payments/authorize", PaymentsServer::handleAuthorize);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("payments-api listening on :" + port);
    }

    private static void handleAuthorize(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "{\"error\":\"method_not_allowed\"}");
            return;
        }
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String orderId = find(ORDER_ID_RE, body, "UNKNOWN");
        String customerId = find(CUSTOMER_RE, body, "C-0000");
        String currency = find(CURRENCY_RE, body, "EUR");

        String payId = "PAY-" + shortHash(orderId + "|" + customerId + "|" + currency);
        String resp = String.format(
                "{\"paymentId\":\"%s\",\"orderId\":\"%s\",\"status\":\"AUTHORIZED\",\"currency\":\"%s\",\"wafInspected\":true}",
                esc(payId), esc(orderId), esc(currency)
        );
        System.out.printf("authorized %s for %s customer=%s currency=%s%n", payId, orderId, customerId, currency);
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
        ex.getResponseHeaders().set("X-Payments-Gateway", "partner-waf-protected");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
