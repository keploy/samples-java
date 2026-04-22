package com.tricentisdemo.sap.customer360.service;

import com.tricentisdemo.sap.customer360.persistence.AuditEvent;
import com.tricentisdemo.sap.customer360.repository.AuditEventRepository;
import com.tricentisdemo.sap.customer360.sap.CorrelationIdInterceptor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Writes one row per service operation for compliance audit + usage analytics.
 *
 * <p>Written synchronously (no {@code @Async}) so the INSERT statement lands
 * in Keploy's captured wire-protocol log on the same request path as the
 * outbound SAP GETs. That determinism matters for replay.
 */
@Service
public class AuditService {

    private final AuditEventRepository repo;

    public AuditService(AuditEventRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public AuditEvent record(String customerId, String operation, Integer latencyMs) {
        return repo.save(new AuditEvent(
            customerId,
            operation,
            MDC.get(CorrelationIdInterceptor.MDC_KEY),
            latencyMs
        ));
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> recent() {
        return repo.findTop50ByOrderByHappenedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> recentForCustomer(String customerId) {
        return repo.findTop20ByCustomerIdOrderByHappenedAtDesc(customerId);
    }
}
