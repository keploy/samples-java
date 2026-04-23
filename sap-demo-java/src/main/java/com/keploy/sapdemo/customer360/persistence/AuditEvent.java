package com.keploy.sapdemo.customer360.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One row per service operation — the compliance audit trail.
 *
 * <p>Written synchronously as part of the aggregator flow so the INSERT
 * statement shows up in Keploy's Postgres wire-protocol capture on the
 * same path as the SAP HTTP GETs. If you reorder this to an async write,
 * Keploy replays may race against the test runner.
 */
@Entity
@Table(name = "audit_event")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", length = 10)
    private String customerId;

    @Column(nullable = false, length = 64)
    private String operation;

    @Column(name = "correlation_id", length = 128)
    private String correlationId;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "happened_at", nullable = false, updatable = false)
    private Instant happenedAt;

    public AuditEvent() {}

    public AuditEvent(String customerId, String operation, String correlationId, Integer latencyMs) {
        this.customerId = customerId;
        this.operation = operation;
        this.correlationId = correlationId;
        this.latencyMs = latencyMs;
        this.happenedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String v) { this.customerId = v; }
    public String getOperation() { return operation; }
    public void setOperation(String v) { this.operation = v; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String v) { this.correlationId = v; }
    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer v) { this.latencyMs = v; }
    public Instant getHappenedAt() { return happenedAt; }
    public void setHappenedAt(Instant v) { this.happenedAt = v; }
}
