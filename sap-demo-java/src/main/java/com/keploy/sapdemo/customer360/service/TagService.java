package com.keploy.sapdemo.customer360.service;

import com.keploy.sapdemo.customer360.persistence.CustomerTag;
import com.keploy.sapdemo.customer360.repository.CustomerTagRepository;
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
        // Single-lookup idempotency: if the row exists, return it; otherwise
        // insert. We retry the lookup on DataIntegrityViolationException to
        // cover the narrow window where a concurrent insert beats us.
        return repo.findByCustomerIdAndTag(customerId, normalised)
            .orElseGet(() -> insertOrFetchExisting(customerId, normalised, createdBy));
    }

    private CustomerTag insertOrFetchExisting(String customerId, String tag, String createdBy) {
        try {
            return repo.save(new CustomerTag(customerId, tag, createdBy));
        } catch (DataIntegrityViolationException race) {
            log.debug("tag add race on ({},{}); fetching existing", customerId, tag);
            return repo.findByCustomerIdAndTag(customerId, tag)
                .orElseThrow(() -> race);
        }
    }

    @Transactional
    public boolean remove(String customerId, String tag) {
        return repo.deleteByCustomerIdAndTag(customerId, tag.trim().toLowerCase()) > 0;
    }
}
