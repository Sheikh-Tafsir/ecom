package com.example.ecom.common.client.stock;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.UUID;

/**
 * Cross-domain contract for stock consumption (called by Order domain).
 * <p>
 * Monolith: implemented by {@code StockClientAdapter} in the stock domain.
 * Microservice: Spring generates an HTTP client via {@code @HttpExchange}.
 */
@HttpExchange("/internal/stock")
public interface StockClient {

    @PostExchange("/{productId}/consume")
    void consume(@PathVariable UUID productId, @RequestParam("quantity") int quantity, @RequestParam("orderId") UUID orderId);
}
