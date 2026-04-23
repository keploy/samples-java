package com.keploy.sapdemo.customer360.web;

import com.keploy.sapdemo.customer360.model.TagRequest;
import com.keploy.sapdemo.customer360.persistence.CustomerTag;
import com.keploy.sapdemo.customer360.service.AuditService;
import com.keploy.sapdemo.customer360.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/tags")
@Validated
@Tag(name = "Tags", description = "Local labels attached to a BP — stored in Postgres, not SAP")
public class TagController {

    private static final Logger log = LoggerFactory.getLogger(TagController.class);

    private static final String ID_REGEX = "^[0-9A-Za-z]{1,10}$";

    private final TagService tags;
    private final AuditService audit;

    public TagController(TagService tags, AuditService audit) {
        this.tags = tags;
        this.audit = audit;
    }

    @Operation(summary = "List tags on a customer (Postgres read)")
    @GetMapping
    public ResponseEntity<List<CustomerTag>> list(
        @PathVariable @NotBlank @Pattern(regexp = ID_REGEX) String customerId
    ) {
        log.info("GET tags for bp={}", customerId);
        List<CustomerTag> result = tags.list(customerId);
        audit.record(customerId, "tags.list", null);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Add a tag to a customer (Postgres insert)")
    @PostMapping
    public ResponseEntity<CustomerTag> add(
        @PathVariable @NotBlank @Pattern(regexp = ID_REGEX) String customerId,
        @Valid @RequestBody TagRequest req
    ) {
        log.info("POST tag={} for bp={}", req.getTag(), customerId);
        String by = req.getCreatedBy() == null || req.getCreatedBy().isBlank() ? "api" : req.getCreatedBy();
        CustomerTag saved = tags.add(customerId, req.getTag(), by);
        audit.record(customerId, "tags.add", null);
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "Remove a tag (Postgres delete)")
    @DeleteMapping("/{tag}")
    public ResponseEntity<Map<String, Object>> remove(
        @PathVariable @NotBlank @Pattern(regexp = ID_REGEX) String customerId,
        @PathVariable @NotBlank String tag
    ) {
        log.info("DELETE tag={} for bp={}", tag, customerId);
        String normalised = tag.trim().toLowerCase();
        boolean deleted = tags.remove(customerId, normalised);
        audit.record(customerId, "tags.delete", null);
        return ResponseEntity.ok(Map.of("deleted", deleted, "tag", normalised));
    }
}
