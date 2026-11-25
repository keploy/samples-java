package com.example.tidbstmtcache;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Event row that the /events/patch endpoint persists before fanning the
 * event out to a partitioned Pulsar topic. The exact column types are
 * deliberately boring — we are not validating Hibernate type mapping
 * here, only that an INSERT + COMMIT goes through MySQL Connector/J in
 * a way the keploy MySQL parser already covers.
 */
@Entity
@Table(name = "events")
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_id", nullable = false, length = 64)
    private String entityId;

    @Column(name = "event_name", nullable = false, length = 64)
    private String eventName;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @Column(name = "task_orchestrator", length = 32)
    private String taskOrchestrator;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Instant eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public String getTaskOrchestrator() {
        return taskOrchestrator;
    }

    public void setTaskOrchestrator(String taskOrchestrator) {
        this.taskOrchestrator = taskOrchestrator;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }
}
