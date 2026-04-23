package com.keploy.sapdemo.customer360.web;

import com.keploy.sapdemo.customer360.model.NoteRequest;
import com.keploy.sapdemo.customer360.persistence.CustomerNote;
import com.keploy.sapdemo.customer360.service.AuditService;
import com.keploy.sapdemo.customer360.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/notes")
@Validated
@Tag(name = "Notes", description = "Free-text notes captured locally in Postgres")
public class NoteController {

    private static final Logger log = LoggerFactory.getLogger(NoteController.class);

    private static final String ID_REGEX = "^[0-9A-Za-z]{1,10}$";

    private final NoteService notes;
    private final AuditService audit;

    public NoteController(NoteService notes, AuditService audit) {
        this.notes = notes;
        this.audit = audit;
    }

    @Operation(summary = "List notes for a customer (Postgres read)")
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
        @PathVariable @NotBlank @Pattern(regexp = ID_REGEX) String customerId
    ) {
        log.info("GET notes for bp={}", customerId);
        List<CustomerNote> result = notes.list(customerId);
        audit.record(customerId, "notes.list", null);
        return ResponseEntity.ok(Map.of(
            "items", result,
            "count", result.size()
        ));
    }

    @Operation(summary = "Add a note to a customer (Postgres insert)")
    @PostMapping
    public ResponseEntity<CustomerNote> add(
        @PathVariable @NotBlank @Pattern(regexp = ID_REGEX) String customerId,
        @Valid @RequestBody NoteRequest req
    ) {
        log.info("POST note len={} for bp={}", req.getBody().length(), customerId);
        String author = req.getAuthor() == null || req.getAuthor().isBlank() ? "api" : req.getAuthor();
        CustomerNote saved = notes.add(customerId, req.getBody(), author);
        audit.record(customerId, "notes.add", null);
        return ResponseEntity.ok(saved);
    }
}
