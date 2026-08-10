package io.keploy.productcatalog.service;

import io.keploy.productcatalog.model.Product;
import io.keploy.productcatalog.repository.ProductRepository;
import io.keploy.productcatalog.web.dto.InventorySummaryResponse;
import io.keploy.productcatalog.web.dto.ProductRequest;
import io.keploy.productcatalog.web.error.InsufficientStockException;
import io.keploy.productcatalog.web.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Product> findAll(String category) {
        if (category != null && !category.isBlank()) {
            return repository.findByCategoryIgnoreCase(category);
        }
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return repository.findById(id)
                // The message wording is part of the recorded 404 contract — the GET/PUT/DELETE
                // not-found tests assert body.message verbatim, so keep the "Product <id> not found" form.
                .orElseThrow(() -> new ResourceNotFoundException("Product " + id + " not found"));
    }

    @Transactional
    public Product create(ProductRequest request) {
        Product product = new Product(
                request.name(),
                request.description(),
                request.price(),
                request.stockQuantity(),
                request.category()
        );
        return repository.save(product);
    }

    @Transactional
    public Product update(Long id, ProductRequest request) {
        Product product = findById(id);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setCategory(request.category());
        return repository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findById(id);
        repository.delete(product);
    }

    /**
     * Applies a relative change to a product's stock. A positive delta restocks, a negative
     * delta ships units. The adjustment is rejected (409) if it would drive stock below zero.
     */
    @Transactional
    public Product adjustStock(Long id, int delta) {
        Product product = findById(id);   // 404 if the product doesn't exist
        int rows = repository.applyStockDelta(id, delta);
        if (rows == 0) {
            // The product exists (checked above), so a 0-row update means the DB-side guard
            // rejected the change: applying the delta would drive stock below zero.
            int current = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            throw new InsufficientStockException(
                    "Cannot adjust stock of product " + id + " by " + delta
                            + ": only " + current + " in stock");
        }
        return findById(id);   // re-read the persisted state (the update cleared the context)
    }

    /**
     * Builds an aggregated inventory summary across the whole catalog: total counts,
     * total value (price * stock), the products at or below the low-stock threshold,
     * and a per-category rollup sorted by inventory value (highest first).
     */
    @Transactional(readOnly = true)
    public InventorySummaryResponse getInventorySummary(int lowStockThreshold) {
        List<Product> products = repository.findAll();

        long totalStockUnits = 0L;
        BigDecimal totalInventoryValue = BigDecimal.ZERO;
        List<String> lowStockProducts = new java.util.ArrayList<>();
        Map<String, CategoryAccumulator> byCategory = new LinkedHashMap<>();

        for (Product product : products) {
            int stock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            BigDecimal price = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
            BigDecimal value = price.multiply(BigDecimal.valueOf(stock));

            totalStockUnits += stock;
            totalInventoryValue = totalInventoryValue.add(value);

            if (stock <= lowStockThreshold) {
                lowStockProducts.add(product.getName());
            }

            String category = (product.getCategory() == null || product.getCategory().isBlank())
                    ? "uncategorized"
                    : product.getCategory();
            byCategory.computeIfAbsent(category, CategoryAccumulator::new).add(stock, value);
        }

        List<InventorySummaryResponse.CategoryBreakdown> categories = byCategory.values().stream()
                .map(CategoryAccumulator::toBreakdown)
                .sorted(Comparator.comparing(
                        InventorySummaryResponse.CategoryBreakdown::inventoryValue).reversed())
                .toList();

        return new InventorySummaryResponse(
                products.size(),
                totalStockUnits,
                totalInventoryValue,
                lowStockThreshold,
                lowStockProducts,
                categories
        );
    }

    private static final class CategoryAccumulator {
        private final String category;
        private long productCount;
        private long stockUnits;
        private BigDecimal inventoryValue = BigDecimal.ZERO;

        private CategoryAccumulator(String category) {
            this.category = category;
        }

        private void add(int stock, BigDecimal value) {
            this.productCount++;
            this.stockUnits += stock;
            this.inventoryValue = this.inventoryValue.add(value);
        }

        private InventorySummaryResponse.CategoryBreakdown toBreakdown() {
            return new InventorySummaryResponse.CategoryBreakdown(
                    category, productCount, stockUnits, inventoryValue);
        }
    }
}
