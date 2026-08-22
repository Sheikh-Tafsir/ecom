package com.example.ecom.order.dto;

import com.example.ecom.common.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(

        @NotNull
        OrderStatus status
) {
}
