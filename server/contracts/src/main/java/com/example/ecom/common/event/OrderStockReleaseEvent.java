package com.example.ecom.common.event;

import java.util.List;
import java.util.UUID;

/**
 * Published when an order is cancelled, rejected, or returned.
 * Consumer (product-service) releases the reserved stock for each item.
 */
public record OrderStockReleaseEvent(
        UUID orderId,
        List<Item> items
) {
    public record Item(UUID productId, int quantity) {}
}
