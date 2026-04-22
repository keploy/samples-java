package com.tricentisdemo.sap.customer360.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * RFC 7807 Problem Details for HTTP APIs response body.
 *
 * <p>Returned by {@link com.tricentisdemo.sap.customer360.web.GlobalExceptionHandler}
 * for any unhandled exception surfaced by the service. Stable shape so that
 * downstream consumers (and Keploy mock diffs) can rely on it.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemResponse {

    private String type;
    private String title;
    private int status;
    private String detail;
    private String instance;
    private String correlationId;
    private Integer upstreamStatus;
    private String sapErrorCode;
    private Instant timestamp;

    public ProblemResponse() {
        this.timestamp = Instant.now();
    }

    public String getType() { return type; }
    public void setType(String v) { this.type = v; }

    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }

    public int getStatus() { return status; }
    public void setStatus(int v) { this.status = v; }

    public String getDetail() { return detail; }
    public void setDetail(String v) { this.detail = v; }

    public String getInstance() { return instance; }
    public void setInstance(String v) { this.instance = v; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String v) { this.correlationId = v; }

    public Integer getUpstreamStatus() { return upstreamStatus; }
    public void setUpstreamStatus(Integer v) { this.upstreamStatus = v; }

    public String getSapErrorCode() { return sapErrorCode; }
    public void setSapErrorCode(String v) { this.sapErrorCode = v; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant v) { this.timestamp = v; }
}
