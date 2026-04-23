package com.tricentisdemo.sap.customer360.web;

import com.tricentisdemo.sap.customer360.persistence.AuditEvent;
import com.tricentisdemo.sap.customer360.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Postgres-only access-audit read surface — no SAP call on this path.
 *
 * <p>Exposes two equivalent routes for the same underlying query so both
 * "audit-centric" consumers ({@code /api/v1/audit}) and the legacy
 * customer-centric UI ({@code /api/v1/customers/recent-views}) resolve
 * without the generic 500-from-NoResourceFoundException trap.
 */
@RestController
@Tag(name = "Audit", description = "Service-level access audit (Postgres read)")
public class AuditController {

    private static final Logger log = LoggerFactory.getLogger(AuditController.class);

    private final AuditService audit;

    public AuditController(AuditService audit) {
        this.audit = audit;
    }

    @Operation(summary = "Most recent 50 audit events across all customers",
               description = "Postgres-only — no SAP call. Useful for compliance/ops dashboards.")
    @GetMapping({"/api/v1/audit", "/api/v1/customers/recent-views"})
    public ResponseEntity<Map<String, Object>> recent() {
        log.info("GET audit recent-views");
        List<AuditEvent> events = audit.recent();
        if (events == null) {
            events = List.of();
        }
        return ResponseEntity.ok(Map.of(
            "items", events,
            "count", events.size()
        ));
    }
}
