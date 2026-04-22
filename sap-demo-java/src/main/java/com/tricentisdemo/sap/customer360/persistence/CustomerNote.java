package com.tricentisdemo.sap.customer360.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Free-text note captured on a customer (e.g. during a CSR call). Multiple
 * notes allowed per customer; ordered by created_at for display.
 */
@Entity
@Table(name = "customer_note")
public class CustomerNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, length = 10)
    private String customerId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false, length = 64)
    private String author;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CustomerNote() {}

    public CustomerNote(String customerId, String body, String author) {
        this.customerId = customerId;
        this.body = body;
        this.author = author;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String v) { this.customerId = v; }
    public String getBody() { return body; }
    public void setBody(String v) { this.body = v; }
    public String getAuthor() { return author; }
    public void setAuthor(String v) { this.author = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
}
