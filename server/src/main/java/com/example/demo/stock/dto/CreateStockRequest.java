package com.example.demo.stock.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateStockRequest(

        @NotEmpty
        List<@Valid CreateStockItemRequest> items
) {
}
