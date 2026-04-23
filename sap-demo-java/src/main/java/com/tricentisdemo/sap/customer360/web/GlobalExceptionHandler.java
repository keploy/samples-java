package com.tricentisdemo.sap.customer360.web;

import com.tricentisdemo.sap.customer360.model.ProblemResponse;
import com.tricentisdemo.sap.customer360.sap.CorrelationIdInterceptor;
import com.tricentisdemo.sap.customer360.sap.SapApiException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Converts uncaught exceptions into RFC 7807 problem responses.
 *
 * <p>Every error path populates:
 * <ul>
 *   <li>the {@code X-Correlation-ID} response header (for cross-system tracing)</li>
 *   <li>the {@code X-Upstream-Status} header when an SAP upstream was
 *       responsible for the failure (Keploy captures this on replay so
 *       downstream tests can assert on SAP-side status transitions)</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SapApiException.class)
    public ResponseEntity<ProblemResponse> handleSap(SapApiException ex, HttpServletRequest req) {
        HttpStatus status = mapSapStatus(ex.getUpstreamStatus());
        ProblemResponse body = baseProblem(status, "SAP upstream error", ex.getMessage(), req);
        body.setUpstreamStatus(ex.getUpstreamStatus().value());
        body.setSapErrorCode(ex.getSapErrorCode());

        log.warn("SAP error surfaced to caller: status={} upstream={} detail={}",
            status.value(), ex.getUpstreamStatus().value(), ex.getMessage());

        return ResponseEntity.status(status)
            .headers(commonHeaders(ex.getUpstreamStatus().value()))
            .body(body);
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ProblemResponse> handleCircuitOpen(CallNotPermittedException ex,
                                                             HttpServletRequest req) {
        log.warn("Circuit breaker open, rejecting call: {}", ex.getMessage());
        ProblemResponse body = baseProblem(
            HttpStatus.SERVICE_UNAVAILABLE,
            "SAP upstream temporarily unavailable",
            "Circuit breaker is open; retry after a short delay.",
            req);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .headers(commonHeaders(null))
            .body(body);
    }

    @ExceptionHandler({
        ConstraintViolationException.class,
        MethodArgumentNotValidException.class,
        IllegalArgumentException.class,
        HttpMessageNotReadableException.class
    })
    public ResponseEntity<ProblemResponse> handleValidation(Exception ex, HttpServletRequest req) {
        ProblemResponse body = baseProblem(HttpStatus.BAD_REQUEST, "Validation failed",
            ex.getMessage(), req);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .headers(commonHeaders(null))
            .body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemResponse> handleNoRoute(NoResourceFoundException ex,
                                                        HttpServletRequest req) {
        // Spring's default flow routes an unmatched URL through the static
        // resource handler, which then throws NoResourceFoundException.
        // Without this handler it falls into the generic Exception catch
        // and surfaces as a 500 — confusing for clients probing nearby
        // routes (e.g. GET /api/v1/audit when they meant
        // /api/v1/customers/recent-views).
        log.info("Unmapped route path={}", req.getRequestURI());
        ProblemResponse body = baseProblem(HttpStatus.NOT_FOUND, "Not Found",
            "No handler is registered for " + req.getRequestURI(), req);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .headers(commonHeaders(null))
            .body(body);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ProblemResponse> handleDb(DataAccessException ex, HttpServletRequest req) {
        log.warn("database error surfaced to caller", ex);
        ProblemResponse body = baseProblem(HttpStatus.SERVICE_UNAVAILABLE,
            "Database error", "Local persistence layer is unavailable.", req);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .headers(commonHeaders(null))
            .body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        String cid = MDC.get(CorrelationIdInterceptor.MDC_KEY);
        // Full exception (message, stack, class) stays server-side only. Clients
        // receive a generic message plus the correlation id, which they can
        // quote back to operators when opening a ticket.
        log.error("Unhandled exception [{}] path={} — check server logs", cid, req.getRequestURI(), ex);
        ProblemResponse body = baseProblem(HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "An unexpected error occurred. Quote the correlationId when contacting support.",
            req);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .headers(commonHeaders(null))
            .body(body);
    }

    // -----------------------------------------------------------------------

    private static ProblemResponse baseProblem(HttpStatus status, String title,
                                               String detail, HttpServletRequest req) {
        ProblemResponse body = new ProblemResponse();
        body.setType("about:blank");
        body.setTitle(title);
        body.setStatus(status.value());
        body.setDetail(detail);
        body.setInstance(req.getRequestURI());
        body.setCorrelationId(MDC.get(CorrelationIdInterceptor.MDC_KEY));
        return body;
    }

    private static HttpHeaders commonHeaders(Integer upstreamStatus) {
        HttpHeaders h = new HttpHeaders();
        String cid = MDC.get(CorrelationIdInterceptor.MDC_KEY);
        if (cid != null) {
            h.set(CorrelationIdInterceptor.HEADER, cid);
        }
        if (upstreamStatus != null) {
            h.set("X-Upstream-Status", String.valueOf(upstreamStatus));
        }
        return h;
    }

    private static HttpStatus mapSapStatus(HttpStatus upstream) {
        // SAP 404 on a specific partner → 404 Not Found to our caller.
        // SAP 401/403 → reflect as 502 (the caller didn't auth wrong, we did).
        // SAP 4xx otherwise → 502 (bad gateway / upstream misconfig).
        // SAP 5xx / timeouts → 502.
        if (upstream == HttpStatus.NOT_FOUND) return HttpStatus.NOT_FOUND;
        if (upstream == HttpStatus.TOO_MANY_REQUESTS) return HttpStatus.TOO_MANY_REQUESTS;
        return HttpStatus.BAD_GATEWAY;
    }
}
