package com.example.tidbstmtcache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Mirrors the Flipkart Global-Shipment-Master /events/patch endpoint:
 * persists an event row through Hibernate (TiDB) and fans it out to a
 * partitioned Pulsar topic. The request/response shapes match the
 * Strowger recording (test post-events-patch-1.yaml) so a recording
 * captured against this sample produces mocks structurally identical
 * to the customer's.
 *
 * Why a synchronous .send() rather than sendAsync(): we want the
 * record-time mock for SEND_RECEIPT and the in-transaction COMMIT to
 * the database to be serialised in the order keploy's recorder
 * observes them on the wire. Async send introduces an extra inflight
 * queue that makes the relative ordering of the MySQL COMMIT and the
 * Pulsar SEND non-deterministic across runs, which would muddy the
 * regression repro for reasons unrelated to the partition router.
 */
@RestController
public class EventsController {

    private final EventRepository eventRepository;
    private final Producer<byte[]> producer;
    private final ObjectMapper objectMapper;

    public EventsController(EventRepository eventRepository,
                            Producer<byte[]> producer,
                            ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.producer = producer;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/events/patch")
    @Transactional
    public Map<String, String> patchEvent(@RequestBody Map<String, Object> body)
            throws PulsarClientException, JsonProcessingException {
        EventEntity entity = new EventEntity();
        entity.setEntityId((String) body.get("entity_id"));
        entity.setEventName((String) body.get("event_name"));
        entity.setTaskOrchestrator((String) body.get("task_orchestrator"));
        entity.setEventTimestamp(OffsetDateTime.parse((String) body.get("event_timestamp")).toInstant());
        entity.setPayloadJson(objectMapper.writeValueAsString(body));

        eventRepository.save(entity);

        // Synchronous send so the SEND_RECEIPT lands inside the same HTTP
        // request boundary. See class doc for why async is avoided.
        producer.send(objectMapper.writeValueAsBytes(body));

        Map<String, String> out = new HashMap<>();
        out.put("message", "Event patched");
        return out;
    }
}
