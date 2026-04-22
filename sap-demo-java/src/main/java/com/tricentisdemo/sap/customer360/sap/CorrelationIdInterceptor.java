package com.tricentisdemo.sap.customer360.sap;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.UUID;

/**
 * Propagates the caller's correlation id into every outbound SAP call.
 *
 * <p>For incoming requests, {@link com.tricentisdemo.sap.customer360.sap.CorrelationIdFilter}
 * seeds the MDC. This interceptor reads it back and sets the
 * {@code X-Correlation-ID} header on the SAP call so distributed traces
 * chain across the hop — operationally critical in BTP landscapes where a
 * single business request can trigger calls to five or more backends.
 *
 * <p>If MDC is empty (e.g., a scheduled job), a fresh id is generated so
 * the SAP side always sees something.
 */
public class CorrelationIdInterceptor implements ClientHttpRequestInterceptor {

    public static final String MDC_KEY = "correlationId";
    public static final String HEADER = "X-Correlation-ID";

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        String correlationId = MDC.get(MDC_KEY);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        request.getHeaders().set(HEADER, correlationId);
        return execution.execute(request, body);
    }
}
