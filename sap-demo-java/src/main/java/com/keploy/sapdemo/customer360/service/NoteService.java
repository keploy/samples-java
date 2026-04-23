package com.keploy.sapdemo.customer360.service;

import com.keploy.sapdemo.customer360.persistence.CustomerNote;
import com.keploy.sapdemo.customer360.repository.CustomerNoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NoteService {

    private final CustomerNoteRepository repo;

    public NoteService(CustomerNoteRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<CustomerNote> list(String customerId) {
        return repo.findAllByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional
    public CustomerNote add(String customerId, String body, String author) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("note body must not be blank");
        }
        if (body.length() > 2000) {
            throw new IllegalArgumentException("note body must be ≤ 2000 chars");
        }
        return repo.save(new CustomerNote(customerId, body.trim(), author));
    }

    @Transactional(readOnly = true)
    public long count(String customerId) {
        return repo.countByCustomerId(customerId);
    }
}
