package com.keploy.sapdemo.customer360.config;

import com.keploy.sapdemo.customer360.sap.CorrelationIdInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * Configures the {@link RestTemplate} used for all outbound calls to the SAP
 * S/4HANA Business Partner OData service.
 *
 * <p>Two interceptors are attached:
 * <ol>
 *   <li><b>SapAuthInterceptor</b> — stamps the required {@code APIKey} header
 *       (sandbox) or {@code Authorization: Bearer} (production tenant).</li>
 *   <li><b>CorrelationIdInterceptor</b> — propagates the inbound request's
 *       correlation id into the outbound SAP call so traces chain across
 *       the hop.</li>
 * </ol>
 *
 * <p>An {@link Executor} bean is also exposed for the
 * {@link com.keploy.sapdemo.customer360.service.Customer360AggregatorService}
 * fan-out pattern. The three SAP OData calls run in parallel; the pool is
 * intentionally small to avoid overwhelming the SAP API manager during
 * regression test runs.
 */
@Configuration
@EnableAsync
public class SapClientConfig {

    private static final Logger log = LoggerFactory.getLogger(SapClientConfig.class);

    @Value("${sap.api.base-url}")
    private String baseUrl;

    @Value("${sap.api.key:}")
    private String apiKey;

    @Value("${sap.api.bearer-token:}")
    private String bearerToken;

    @Value("${sap.api.connect-timeout-seconds:10}")
    private int connectTimeoutSeconds;

    @Value("${sap.api.read-timeout-seconds:30}")
    private int readTimeoutSeconds;

    @Bean
    public RestTemplate sapRestTemplate(RestTemplateBuilder builder) {
        log.info("Configuring SAP RestTemplate: baseUrl={}, connectTimeout={}s, readTimeout={}s, authMode={}",
            baseUrl, connectTimeoutSeconds, readTimeoutSeconds,
            !bearerToken.isBlank() ? "bearer" : !apiKey.isBlank() ? "apikey" : "NONE");

        if (apiKey.isBlank() && bearerToken.isBlank()) {
            log.warn("No SAP credentials configured (SAP_API_KEY / SAP_BEARER_TOKEN both empty). "
                + "Outbound SAP calls will fail with 401.");
        }

        // Spring Boot auto-selects HttpClient5 when httpclient5 is on the
        // classpath (see pom.xml). Setting connect+read timeouts via the
        // RestTemplateBuilder plumbs them through to the underlying
        // HttpClient5 RequestConfig (connect) and SocketConfig (soTimeout ==
        // read timeout), which is the only API surface that still exists in
        // Spring 6 — HttpComponentsClientHttpRequestFactory.setReadTimeout
        // was removed with the HttpClient5 migration.
        return builder
            .rootUri(baseUrl)
            .setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
            .setReadTimeout(Duration.ofSeconds(readTimeoutSeconds))
            .additionalInterceptors(
                new SapAuthInterceptor(apiKey, bearerToken),
                new CorrelationIdInterceptor()
            )
            .build();
    }

    @Bean(name = "sapCallExecutor")
    public Executor sapCallExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(32);
        executor.setThreadNamePrefix("sap-call-");
        executor.setAllowCoreThreadTimeOut(true);
        executor.initialize();
        return executor;
    }

    /**
     * Adds {@code APIKey} (sandbox) or {@code Authorization: Bearer} (production)
     * plus a standard {@code Accept: application/json} on every outbound call.
     */
    static final class SapAuthInterceptor implements ClientHttpRequestInterceptor {
        private final String apiKey;
        private final String bearerToken;

        SapAuthInterceptor(String apiKey, String bearerToken) {
            this.apiKey = apiKey;
            this.bearerToken = bearerToken;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            HttpHeaders headers = request.getHeaders();
            if (!bearerToken.isBlank()) {
                headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
            } else if (!apiKey.isBlank()) {
                headers.set("APIKey", apiKey);
            }
            if (!headers.containsKey(HttpHeaders.ACCEPT)) {
                headers.set(HttpHeaders.ACCEPT, "application/json");
            }
            return execution.execute(request, body);
        }
    }

}
