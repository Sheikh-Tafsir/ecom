package com.example.demo.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateOrderItemRequest(

        @NotNull
        Long productId,

        @Min(1)
        int quantity
) {
}
