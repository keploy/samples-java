package com.tricentisdemo.sap.customer360.sap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Inbound correlation-id filter. Runs first in the filter chain.
 *
 * <p>If the caller supplies an {@code X-Correlation-ID} header (typical for
 * requests routed through an API gateway or upstream BTP service), we honour
 * it. Otherwise we mint a new one. Either way, it lands in MDC for the
 * duration of the request so every log line carries it, and is echoed back
 * on the response so the caller can correlate on their side.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String MDC_KEY = CorrelationIdInterceptor.MDC_KEY;
    public static final String HEADER = CorrelationIdInterceptor.HEADER;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
