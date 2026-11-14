package com.example.ecom.product.stock.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateStockRequest(
        @NotEmpty
        List<@Valid UpdateStockItemRequest> items
) {
}
