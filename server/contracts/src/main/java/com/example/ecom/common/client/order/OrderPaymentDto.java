package com.example.ecom.common.client.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Order data exposed to other domains (e.g., Payment).
 */
public record OrderPaymentDto(
        UUID id,
        UUID userId,
        String status,
        BigDecimal totalPrice,
        boolean paid,
        List<OrderItemPaymentDto> items
) {
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_REJECTED = "REJECTED";

    public boolean isCancelledOrRejected() {
        return STATUS_CANCELLED.equalsIgnoreCase(status) || STATUS_REJECTED.equalsIgnoreCase(status);
    }
}
