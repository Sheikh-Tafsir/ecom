package com.example.demo.stock.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateStockItemRequest(
        Long id,

        @Min(1)
        int quantity,

        @NotNull
        BigDecimal cost
) {
}
