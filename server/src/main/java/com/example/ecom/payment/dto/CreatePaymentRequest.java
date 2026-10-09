package com.example.ecom.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
        UUID userId,
        UUID orderId,
        BigDecimal amount,
        String payerReference
) {
}
