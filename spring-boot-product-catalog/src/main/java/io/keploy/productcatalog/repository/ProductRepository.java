package io.keploy.productcatalog.repository;

import io.keploy.productcatalog.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoryIgnoreCase(String category);

    /**
     * Atomically applies a relative change to a product's stock in a single guarded UPDATE.
     * Doing the read-modify-write in the database (instead of load-mutate-save) means concurrent
     * adjustments cannot clobber each other, and the {@code >= 0} guard cannot be bypassed under a
     * race. Returns the number of rows updated: {@code 0} means the row was absent or the guard
     * rejected the change (it would drive stock below zero).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.stockQuantity = p.stockQuantity + :delta "
            + "where p.id = :id and p.stockQuantity + :delta >= 0")
    int applyStockDelta(@Param("id") Long id, @Param("delta") int delta);
}
