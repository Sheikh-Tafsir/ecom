package com.example.demo.order.dto;

import jakarta.validation.constraints.Min;

public record UpdateOrderItemRequest(

        @Min(1)
        int quantity
) {
}
