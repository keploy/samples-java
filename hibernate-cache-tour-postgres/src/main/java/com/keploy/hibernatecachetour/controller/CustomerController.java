package com.keploy.hibernatecachetour.controller;

import com.keploy.hibernatecachetour.model.Customer;
import com.keploy.hibernatecachetour.model.CustomerTag;
import com.keploy.hibernatecachetour.repository.CustomerRepository;
import com.keploy.hibernatecachetour.repository.CustomerTagRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST surface that exercises the prepared-statement bind paths the v3
 * codec must reconcile.
 *
 * Endpoints:
 *   POST   /customer            create + return id
 *   GET    /customer/{id}       single-bind WHERE id = ? (the canonical
 *                               format-flip trigger)
 *   GET    /customer/{id}/tags  WHERE customer_id = ?
 *   GET    /tags?priority=N     WHERE priority = ?
 *   POST   /tag                 insert + return id
 *
 * Each GET is intentionally a JpaRepository finder that compiles to a
 * single-int-bind prepared statement. The exerciser hits each one >=8
 * times (CI script) so pgjdbc's prepareThreshold (default 5) trips and
 * the bind-format byte flips text -> binary mid-recording.
 *
 * The X-Keploy-Test-Name header (forwarded by the keploy recorder when
 * configured with keploy.io/test-name) is read by an interceptor that
 * evicts the L2 cache between logical tests — that's how issue #3
 * (StatementCache classification drift) gets exercised deterministically.
 */
@RestController
public class CustomerController {

    private final CustomerRepository customers;
    private final CustomerTagRepository tags;

    @PersistenceContext
    private EntityManager em;

    public CustomerController(CustomerRepository customers, CustomerTagRepository tags) {
        this.customers = customers;
        this.tags = tags;
    }

    @PostMapping("/customer")
    @Transactional
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, String> body) {
        Customer c = new Customer(body.getOrDefault("name", "anon"),
                                   body.getOrDefault("email", "anon@example.com"));
        Customer saved = customers.save(c);
        return ResponseEntity.ok(Map.of("id", saved.getId(), "name", saved.getName()));
    }

    @GetMapping("/customer/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> get(@PathVariable Integer id) {
        return customers.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "not found")));
    }

    @GetMapping("/customer/{id}/tags")
    @Transactional(readOnly = true)
    public ResponseEntity<List<CustomerTag>> tagsFor(@PathVariable Integer id) {
        return ResponseEntity.ok(tags.findByCustomerId(id));
    }

    @GetMapping("/tags")
    @Transactional(readOnly = true)
    public ResponseEntity<List<CustomerTag>> tagsByPriority(@RequestParam("priority") Integer priority) {
        return ResponseEntity.ok(tags.findByPriority(priority));
    }

    @PostMapping("/tag")
    @Transactional
    public ResponseEntity<Map<String, Object>> createTag(@RequestBody Map<String, Object> body) {
        Integer customerId = (Integer) body.get("customerId");
        String tag = (String) body.getOrDefault("tag", "untagged");
        Integer priority = (Integer) body.getOrDefault("priority", 1);
        CustomerTag t = new CustomerTag(customerId, tag, priority);
        CustomerTag saved = tags.save(t);
        return ResponseEntity.ok(Map.of("id", saved.getId(), "tag", saved.getTag()));
    }
}
