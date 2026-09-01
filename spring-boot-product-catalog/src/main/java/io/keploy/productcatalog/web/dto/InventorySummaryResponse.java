package io.keploy.productcatalog.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregated view of the catalog's inventory. Computed on the fly from all products
 * so the demo can showcase Keploy recording responses that depend on internal logic
 * (aggregation, filtering, per-category rollups) rather than a straight DB read-back.
 */
public record InventorySummaryResponse(
        long totalProducts,
        long totalStockUnits,
        BigDecimal totalInventoryValue,
        int lowStockThreshold,
        List<String> lowStockProducts,
        List<CategoryBreakdown> categories
) {
    public record CategoryBreakdown(
            String category,
            long productCount,
            long stockUnits,
            BigDecimal inventoryValue
    ) {
    }
}
