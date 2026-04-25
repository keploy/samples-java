package com.keploy.hibernatecachetour.repository;

import com.keploy.hibernatecachetour.model.Customer;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;

/**
 * JpaRepository.findById issues a prepared statement with a single int4
 * bind in the WHERE clause — this is exactly the pattern that flips
 * pgjdbc format text->binary at prepareThreshold (default 5).
 *
 * QueryHints HIBERNATE_CACHEABLE keeps the query result in the query
 * cache so per-test eviction matters. Without this hint, only entity
 * loads (findById) populate the L2 entity cache; with it, the WHERE
 * clause query result itself is cached too — enlarging the surface
 * for issue #3 to manifest.
 */
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    @Override
    @QueryHints({@QueryHint(name = "org.hibernate.cacheable", value = "true")})
    java.util.Optional<Customer> findById(Integer id);
}
