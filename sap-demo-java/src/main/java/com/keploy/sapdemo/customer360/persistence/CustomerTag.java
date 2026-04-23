package com.keploy.sapdemo.customer360.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * User-assigned label attached to a customer (BP). Unique per (customer_id, tag).
 */
@Entity
@Table(
    name = "customer_tag",
    uniqueConstraints = @UniqueConstraint(name = "uk_customer_tag", columnNames = {"customer_id", "tag"})
)
public class CustomerTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, length = 10)
    private String customerId;

    @Column(nullable = false, length = 64)
    private String tag;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;

    public CustomerTag() {}

    public CustomerTag(String customerId, String tag, String createdBy) {
        this.customerId = customerId;
        this.tag = tag;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String v) { this.customerId = v; }
    public String getTag() { return tag; }
    public void setTag(String v) { this.tag = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { this.createdBy = v; }
}
