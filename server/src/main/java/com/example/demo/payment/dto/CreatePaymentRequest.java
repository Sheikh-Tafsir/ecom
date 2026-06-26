package com.example.demo.payment.dto;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        Long userId,
        Long orderId,
        BigDecimal amount,
        String payerReference
) {
}
