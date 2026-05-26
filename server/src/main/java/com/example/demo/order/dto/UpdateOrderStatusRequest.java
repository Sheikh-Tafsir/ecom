package com.example.demo.order.dto;

import com.example.demo.common.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(

        @NotNull
        OrderStatus status
) {
}
