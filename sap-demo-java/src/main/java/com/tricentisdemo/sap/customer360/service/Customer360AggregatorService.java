package com.tricentisdemo.sap.customer360.service;

import com.tricentisdemo.sap.customer360.model.BusinessPartner;
import com.tricentisdemo.sap.customer360.model.BusinessPartnerAddress;
import com.tricentisdemo.sap.customer360.model.BusinessPartnerRole;
import com.tricentisdemo.sap.customer360.model.Customer360View;
import com.tricentisdemo.sap.customer360.persistence.CustomerNote;
import com.tricentisdemo.sap.customer360.persistence.CustomerTag;
import com.tricentisdemo.sap.customer360.sap.CorrelationIdInterceptor;
import com.tricentisdemo.sap.customer360.sap.SapApiException;
import com.tricentisdemo.sap.customer360.sap.SapBusinessPartnerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Fan-out aggregator composing a 360° view from SAP + local Postgres.
 *
 * <p>Each inbound {@code GET /api/v1/customers/{id}/360} now produces:
 * <pre>
 *   1 inbound HTTP
 *       │
 *       ├─ 3 parallel HTTPS GETs to SAP OData
 *       │     GET /A_BusinessPartner('{id}')
 *       │     GET /A_BusinessPartner('{id}')/to_BusinessPartnerAddress
 *       │     GET /A_BusinessPartner('{id}')/to_BusinessPartnerRole
 *       │
 *       ├─ 2 parallel Postgres SELECTs
 *       │     SELECT … FROM customer_tag   WHERE customer_id = ?
 *       │     SELECT … FROM customer_note  WHERE customer_id = ?
 *       │
 *       └─ 1 Postgres INSERT (audit)
 *             INSERT INTO audit_event (…)
 * </pre>
 *
 * <p>That's the full story for Keploy: every call on the request path —
 * HTTP and Postgres wire-protocol — is visible to eBPF on the host and
 * gets captured into the replay mocks. Tosca can only assert on the
 * rendered tile; Keploy sees all six backend conversations.
 *
 * <p><b>Partial-failure policy:</b> the SAP partner fetch is mandatory.
 * Everything else (addresses, roles, tags, notes) is optional — failure
 * degrades the view rather than the request.
 */
@Service
public class Customer360AggregatorService {

    private static final Logger log = LoggerFactory.getLogger(Customer360AggregatorService.class);

    private final SapBusinessPartnerClient sapClient;
    private final TagService tagService;
    private final NoteService noteService;
    private final AuditService auditService;
    private final Executor fanoutExecutor;

    @Value("${customer360.aggregate-timeout-seconds:25}")
    private int aggregateTimeoutSeconds;

    @Value("${sap.api.base-url}")
    private String dataSourceHint;

    public Customer360AggregatorService(SapBusinessPartnerClient sapClient,
                                        TagService tagService,
                                        NoteService noteService,
                                        AuditService auditService,
                                        @Qualifier("sapCallExecutor") Executor fanoutExecutor) {
        this.sapClient = sapClient;
        this.tagService = tagService;
        this.noteService = noteService;
        this.auditService = auditService;
        this.fanoutExecutor = fanoutExecutor;
    }

    public Customer360View aggregate(String customerId) {
        Instant start = Instant.now();
        String correlationId = MDC.get(CorrelationIdInterceptor.MDC_KEY);
        log.info("Aggregating 360 view for customerId={} correlationId={}", customerId, correlationId);

        // ---- Mandatory: SAP partner --------------------------------------
        BusinessPartner partner = sapClient.fetchPartner(customerId);

        // ---- Parallel fan-out: 2 SAP + 2 Postgres ------------------------
        CompletableFuture<List<BusinessPartnerAddress>> addressesF = async(correlationId,
            () -> safely("addresses", () -> sapClient.fetchAddresses(customerId)));

        CompletableFuture<List<BusinessPartnerRole>> rolesF = async(correlationId,
            () -> safely("roles", () -> sapClient.fetchRoles(customerId)));

        CompletableFuture<List<CustomerTag>> tagsF = async(correlationId,
            () -> safely("tags", () -> tagService.list(customerId)));

        CompletableFuture<List<CustomerNote>> notesF = async(correlationId,
            () -> safely("notes", () -> noteService.list(customerId)));

        List<BusinessPartnerAddress> addresses;
        List<BusinessPartnerRole> roles;
        List<CustomerTag> tags;
        List<CustomerNote> notes;
        try {
            CompletableFuture.allOf(addressesF, rolesF, tagsF, notesF)
                .get(aggregateTimeoutSeconds, TimeUnit.SECONDS);
            addresses = addressesF.get();
            roles = rolesF.get();
            tags = tagsF.get();
            notes = notesF.get();
        } catch (TimeoutException e) {
            log.warn("Aggregation timeout for bp={} after {}s — returning partial view",
                customerId, aggregateTimeoutSeconds);
            addresses = addressesF.getNow(List.of());
            roles = rolesF.getNow(List.of());
            tags = tagsF.getNow(List.of());
            notes = notesF.getNow(List.of());
        } catch (InterruptedException e) {
            // Re-assert interrupt so callers higher up can observe it.
            Thread.currentThread().interrupt();
            log.warn("Aggregation interrupted for bp={}: {}", customerId, e.getMessage());
            addresses = addressesF.getNow(List.of());
            roles = rolesF.getNow(List.of());
            tags = tagsF.getNow(List.of());
            notes = notesF.getNow(List.of());
        } catch (ExecutionException e) {
            // Execution failed inside an async stage; the interrupt flag of
            // the caller is unaffected, so do NOT call Thread.interrupt().
            log.warn("Aggregation failed for bp={}: {}", customerId, e.getMessage());
            addresses = addressesF.getNow(List.of());
            roles = rolesF.getNow(List.of());
            tags = tagsF.getNow(List.of());
            notes = notesF.getNow(List.of());
        }

        int elapsed = (int) Duration.between(start, Instant.now()).toMillis();

        // ---- Audit INSERT (synchronous — part of the recorded path) ------
        auditService.record(customerId, "customer.360", elapsed);

        Customer360View view = new Customer360View();
        view.setCustomerId(customerId);
        view.setPartner(partner);
        view.setAddresses(addresses);
        view.setRoles(roles);
        view.setTags(tags);
        view.setNotes(notes);
        view.setAggregatedAt(Instant.now());
        view.setCorrelationId(correlationId);
        view.setDataSource(dataSourceHint);
        view.setElapsedMs(elapsed);

        log.info("360 aggregated bp={} addresses={} roles={} tags={} notes={} took={}ms",
            customerId, addresses.size(), roles.size(), tags.size(), notes.size(), elapsed);

        return view;
    }

    // ---- helpers -----------------------------------------------------------

    private <T> CompletableFuture<List<T>> async(String correlationId, java.util.function.Supplier<List<T>> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            if (correlationId != null) MDC.put(CorrelationIdInterceptor.MDC_KEY, correlationId);
            try {
                return supplier.get();
            } finally {
                MDC.remove(CorrelationIdInterceptor.MDC_KEY);
            }
        }, fanoutExecutor);
    }

    private static <T> List<T> safely(String what, java.util.function.Supplier<List<T>> supplier) {
        try {
            return supplier.get();
        } catch (SapApiException sap) {
            log.warn("{} failed (SAP): {}", what, sap.getMessage());
            return List.of();
        } catch (RuntimeException e) {
            log.warn("{} failed ({}): {}", what, e.getClass().getSimpleName(), e.getMessage());
            return List.of();
        }
    }
}
