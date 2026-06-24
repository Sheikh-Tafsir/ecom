package com.example.demo.payment.dto;

public record CreatePaymentRequest(
        String userId,
        String amount
) {
}
