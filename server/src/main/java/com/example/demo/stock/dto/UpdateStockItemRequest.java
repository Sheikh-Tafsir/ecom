package com.example.demo.stock.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateStockItemRequest(
        Long id,

        @NotNull
        @Min(1)
        Integer quantity,

        @NotNull
        BigDecimal purchasePrice
) {
}
