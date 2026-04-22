package com.tricentisdemo.sap.customer360.repository;

import com.tricentisdemo.sap.customer360.persistence.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findTop50ByOrderByHappenedAtDesc();

    List<AuditEvent> findTop20ByCustomerIdOrderByHappenedAtDesc(String customerId);
}
