package com.keploy.sapdemo.customer360.repository;

import com.keploy.sapdemo.customer360.persistence.CustomerNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerNoteRepository extends JpaRepository<CustomerNote, Long> {

    List<CustomerNote> findAllByCustomerIdOrderByCreatedAtDesc(String customerId);

    long countByCustomerId(String customerId);
}
