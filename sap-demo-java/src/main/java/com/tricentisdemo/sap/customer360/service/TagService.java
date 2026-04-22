package com.tricentisdemo.sap.customer360.service;

import com.tricentisdemo.sap.customer360.persistence.CustomerTag;
import com.tricentisdemo.sap.customer360.repository.CustomerTagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TagService {

    private static final Logger log = LoggerFactory.getLogger(TagService.class);

    private final CustomerTagRepository repo;

    public TagService(CustomerTagRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<CustomerTag> list(String customerId) {
        return repo.findAllByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional
    public CustomerTag add(String customerId, String tag, String createdBy) {
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("tag must not be blank");
        }
        String normalised = tag.trim().toLowerCase();
        if (repo.existsByCustomerIdAndTag(customerId, normalised)) {
            // Idempotent — return the existing row instead of 409'ing.
            return repo.findAllByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .filter(t -> t.getTag().equals(normalised))
                .findFirst()
                .orElseThrow();
        }
        try {
            return repo.save(new CustomerTag(customerId, normalised, createdBy));
        } catch (DataIntegrityViolationException race) {
            // Unique constraint lost a race to another thread — treat as
            // already-present.
            log.debug("tag add race on ({},{}); fetching existing", customerId, normalised);
            return repo.findAllByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .filter(t -> t.getTag().equals(normalised))
                .findFirst()
                .orElseThrow();
        }
    }

    @Transactional
    public boolean remove(String customerId, String tag) {
        return repo.deleteByCustomerIdAndTag(customerId, tag.trim().toLowerCase()) > 0;
    }
}
