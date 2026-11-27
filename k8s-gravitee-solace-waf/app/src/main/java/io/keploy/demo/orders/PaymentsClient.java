package io.keploy.demo.orders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Calls partner payments-api OUTBOUND through the ModSecurity / OWASP CRS WAF Ingress
 * (Host: payments.partner.local). A payload tripping the partner WAF answers 403.
 */
@Component
class PaymentsClient {
    private static final Logger log = LoggerFactory.getLogger(PaymentsClient.class);

    record Result(int status, String paymentId) {}

    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final ObjectMapper mapper;
    private final String authorizeUrl;
    private final String hostHeader;

    PaymentsClient(ObjectMapper mapper,
                   @Value("${payments.url}") String authorizeUrl,
                   @Value("${payments.host-header}") String hostHeader) {
        this.mapper = mapper;
        this.authorizeUrl = authorizeUrl;
        this.hostHeader = hostHeader;
    }

    Result authorize(String orderId, String customerId, BigDecimal amount, String currency, String note) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", orderId);
        payload.put("customerId", customerId);
        payload.put("amount", amount);
        payload.put("currency", currency);
        payload.put("note", note);
        String body = mapper.writeValueAsString(payload);

        HttpRequest req = HttpRequest.newBuilder(URI.create(authorizeUrl))
                .timeout(Duration.ofSeconds(5))
                .header("Host", hostHeader)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        String payId = null;
        if (resp.statusCode() / 100 == 2 && resp.body() != null && !resp.body().isBlank()) {
            JsonNode node = mapper.readTree(resp.body());
            if (node.hasNonNull("paymentId")) {
                payId = node.get("paymentId").asText();
            }
        }
        log.info("payments authorize {} via WAF ({}) -> HTTP {} payId={}", orderId, hostHeader, resp.statusCode(), payId);
        return new Result(resp.statusCode(), payId);
    }
}
