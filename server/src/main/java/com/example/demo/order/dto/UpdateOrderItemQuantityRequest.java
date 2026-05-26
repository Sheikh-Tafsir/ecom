package com.example.demo.order.dto;

import jakarta.validation.constraints.Min;

public record UpdateOrderItemQuantityRequest(

        @Min(1)
        int quantity
) {
}
