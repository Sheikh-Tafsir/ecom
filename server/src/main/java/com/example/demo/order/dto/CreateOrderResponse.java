package com.example.demo.order.dto;

import java.math.BigDecimal;

public record CreateOrderResponse(
        long id,
        BigDecimal amount
) {
}
