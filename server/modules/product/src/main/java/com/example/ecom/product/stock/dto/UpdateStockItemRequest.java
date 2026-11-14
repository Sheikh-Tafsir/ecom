package com.example.ecom.product.stock.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateStockItemRequest(

        UUID id,

        @NotNull
        @Min(1)
        Integer quantity,

        @NotNull
        BigDecimal purchasePrice
) {
}
