package com.keploy.sapdemo.customer360.repository;

import com.keploy.sapdemo.customer360.persistence.CustomerTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerTagRepository extends JpaRepository<CustomerTag, Long> {

    List<CustomerTag> findAllByCustomerIdOrderByCreatedAtDesc(String customerId);

    Optional<CustomerTag> findByCustomerIdAndTag(String customerId, String tag);

    @Modifying
    @Transactional
    @Query("DELETE FROM CustomerTag t WHERE t.customerId = :customerId AND t.tag = :tag")
    int deleteByCustomerIdAndTag(@Param("customerId") String customerId, @Param("tag") String tag);
}
