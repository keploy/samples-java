package com.keploy.hibernatecachetour.repository;

import com.keploy.hibernatecachetour.model.CustomerTag;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;

/**
 * Two derived finders — each compiled to a separate prepared statement
 * by Hibernate, each with a single int4 bind. Calling them >=5 times
 * with the same id-space drives BOTH SQL handles past prepareThreshold.
 */
public interface CustomerTagRepository extends JpaRepository<CustomerTag, Integer> {

    @QueryHints({@QueryHint(name = "org.hibernate.cacheable", value = "true")})
    List<CustomerTag> findByCustomerId(Integer customerId);

    @QueryHints({@QueryHint(name = "org.hibernate.cacheable", value = "true")})
    List<CustomerTag> findByPriority(Integer priority);
}
