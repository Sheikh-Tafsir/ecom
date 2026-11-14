package com.example.ecom.common.client.product;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Lightweight product data exposed across domain boundaries.
 *
 * @param id the unique identifier of the product
 * @param name the product name
 * @param firstImage the URL or path to the primary product image
 * @param price the price of the product
 * @param quantity the current available / sellable stock quantity
 */
public record ProductBasicDto(
        UUID id,
        String name,
        String firstImage,
        BigDecimal price,
        int quantity
) {
}
