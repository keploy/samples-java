package io.keploy.productcatalog.web.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Payload for adjusting a product's stock by a relative delta. A positive delta restocks,
 * a negative delta ships/consumes units. Rejecting a delta that would drive stock below zero
 * gives the demo a clean 409 (conflict) traffic case for Keploy to record and replay.
 */
public record StockAdjustmentRequest(

        @NotNull(message = "delta is required")
        Integer delta
) {
}
