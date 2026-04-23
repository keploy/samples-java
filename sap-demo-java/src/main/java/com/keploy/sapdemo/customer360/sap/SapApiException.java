package com.keploy.sapdemo.customer360.sap;

import org.springframework.http.HttpStatus;

/**
 * Application-level exception that translates SAP-side failures into something
 * the Spring MVC layer can map to a meaningful HTTP status.
 *
 * <p>Thrown by the {@link SapBusinessPartnerClient} when:
 * <ul>
 *   <li>SAP returns a 4xx/5xx response (status preserved verbatim)</li>
 *   <li>A transport-level error occurs (connect timeout, read timeout,
 *       TLS failure — mapped to 502 Bad Gateway)</li>
 *   <li>A response body cannot be deserialised to the expected model
 *       (mapped to 502 Bad Gateway with a schema-drift hint)</li>
 * </ul>
 *
 * <p>The {@link #upstreamStatus} field preserves the exact status SAP returned
 * so {@link com.keploy.sapdemo.customer360.web.GlobalExceptionHandler}
 * can surface it in an RFC 7807 problem response as
 * {@code X-Upstream-Status}. Keploy captures this header verbatim in the
 * replayed mocks, which lets contract-diff checks catch status regressions
 * even when the body hasn't changed.
 */
public class SapApiException extends RuntimeException {

    private final HttpStatus upstreamStatus;
    private final String sapErrorCode;

    /**
     * Minimal constructor — upstream HTTP status + message only.
     * Use this for SAP-returned 4xx/5xx errors where no further code or
     * underlying cause is available.
     */
    public SapApiException(HttpStatus upstreamStatus, String message) {
        super(message);
        this.upstreamStatus = upstreamStatus;
        this.sapErrorCode = null;
    }

    /**
     * With SAP error code (from the OData error envelope, e.g.
     * {@code "error.code": "SY/530"}) for richer client diagnostics.
     */
    public SapApiException(HttpStatus upstreamStatus, String sapErrorCode, String message) {
        super(message);
        this.upstreamStatus = upstreamStatus;
        this.sapErrorCode = sapErrorCode;
    }

    /**
     * For transport-level failures that wrap an underlying cause
     * (IOException, deserialisation failure, etc.).
     */
    public SapApiException(HttpStatus upstreamStatus, String message, Throwable cause) {
        super(message, cause);
        this.upstreamStatus = upstreamStatus;
        this.sapErrorCode = null;
    }

    public HttpStatus getUpstreamStatus() {
        return upstreamStatus;
    }

    public String getSapErrorCode() {
        return sapErrorCode;
    }
}
