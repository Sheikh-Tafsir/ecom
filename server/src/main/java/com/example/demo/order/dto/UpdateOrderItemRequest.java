package com.example.demo.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderItemRequest(

        @NotNull
        @Min(1)
        Integer quantity
) {
}
