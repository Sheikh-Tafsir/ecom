package com.example.ecom.common.client.product;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.UUID;

/**
 * Cross-domain contract for product operations needed by other domains.
 * <p>
 * Monolith: implemented by {@code ProductClientAdapter} in the product domain.
 * Microservice: Spring generates an HTTP client via {@code @HttpExchange}.
 */
@HttpExchange("/internal/products")
public interface ProductClient {

    @GetExchange("/{id}")
    ProductBasicDto findProductById(@PathVariable UUID id);

    @GetExchange("/{id}/has-stock/{quantity}")
    boolean hasStock(@PathVariable UUID id, @PathVariable int quantity);

    @PostExchange("/{id}/reserve-stock/{quantity}")
    void reserveStock(@PathVariable UUID id, @PathVariable int quantity);

    @PostExchange("/{id}/release-stock/{quantity}")
    void releaseStock(@PathVariable UUID id, @PathVariable int quantity);
}
