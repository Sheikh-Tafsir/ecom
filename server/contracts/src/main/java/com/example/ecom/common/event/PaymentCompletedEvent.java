package com.example.ecom.common.event;

import java.util.UUID;

/**
 * Published when a payment is successfully executed on the payment gateway (e.g., bKash).
 * Consumer (order-service) accepts the order and marks it as paid.
 */
public record PaymentCompletedEvent(
        UUID orderId,
        String paymentId,
        String transactionId
) {}
