package io.keploy.productcatalog.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload for creating or updating a product. Validation annotations here let the
 * demo showcase both happy-path and 400 (validation error) traffic for Keploy to record.
 */
public record ProductRequest(

        @NotBlank(message = "name is required")
        @Size(max = 120, message = "name must be at most 120 characters")
        String name,

        @Size(max = 1000, message = "description must be at most 1000 characters")
        String description,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "price must be greater than 0")
        BigDecimal price,

        @NotNull(message = "stockQuantity is required")
        @Min(value = 0, message = "stockQuantity cannot be negative")
        Integer stockQuantity,

        String category
) {
}
