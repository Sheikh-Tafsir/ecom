package com.example.ecom.order.dto;

import java.math.BigDecimal;

public record CreateOrderResponse(
        long id,
        BigDecimal totalPrice
) {
}
