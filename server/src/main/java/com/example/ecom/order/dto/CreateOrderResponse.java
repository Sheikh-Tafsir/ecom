package com.example.ecom.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderResponse(
        UUID id,
        BigDecimal totalPrice
) {
}
