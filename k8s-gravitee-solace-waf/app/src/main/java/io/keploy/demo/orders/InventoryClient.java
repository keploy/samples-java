package io.keploy.demo.orders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calls internal inventory-api OUTBOUND through Gravitee APIM Gateway (/inventory/reserve).
 */
@Component
class InventoryClient {
    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);

    record Result(int status, String reservationId, String gatewayTxId) {}

    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final ObjectMapper mapper;
    private final String reserveUrl;
    private final String apiKey;

    InventoryClient(ObjectMapper mapper,
                    @Value("${inventory.url}") String reserveUrl,
                    @Value("${inventory.api-key}") String apiKey) {
        this.mapper = mapper;
        this.reserveUrl = reserveUrl;
        this.apiKey = apiKey;
    }

    Result reserve(String orderId, List<Map<String, Object>> items, String correlationId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", orderId);
        payload.put("warehouse", "EU-WEST-1");
        payload.put("items", items);
        String body = mapper.writeValueAsString(payload);

        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(reserveUrl))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("X-Api-Key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (correlationId != null && !correlationId.isBlank()) {
            req.header("X-Correlation-ID", correlationId);
        }
        HttpResponse<String> resp = http.send(req.build(), HttpResponse.BodyHandlers.ofString());
        String gwTx = resp.headers().firstValue("X-Gravitee-Transaction-Id").orElse(null);
        String resId = null;
        if (resp.statusCode() / 100 == 2 && resp.body() != null && !resp.body().isBlank()) {
            JsonNode node = mapper.readTree(resp.body());
            if (node.hasNonNull("reservationId")) {
                resId = node.get("reservationId").asText();
            }
        }
        log.info("inventory reserve {} via Gravitee -> HTTP {} resId={} gwTx={}", orderId, resp.statusCode(), resId, gwTx);
        return new Result(resp.statusCode(), resId, gwTx);
    }
}
