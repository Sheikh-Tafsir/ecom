package com.example.ecom.common.client.order;

import java.util.UUID;

/**
 * Order item data exposed across domain boundaries.
 */
public record OrderItemPaymentDto(
        UUID productId,
        int quantity
) {
}
