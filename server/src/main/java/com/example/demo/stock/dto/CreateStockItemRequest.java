package com.example.demo.stock.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateStockItemRequest(

        @NotNull
        Long productId,

        @Min(1)
        int quantity,

        @NotNull
        BigDecimal cost
) {
}
