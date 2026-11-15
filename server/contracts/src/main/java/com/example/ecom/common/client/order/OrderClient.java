package com.example.ecom.common.client.order;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.UUID;

/**
 * Cross-domain contract for order operations needed by Payment domain.
 * <p>
 * Monolith: implemented by {@code OrderClientAdapter} in the order domain.
 * Microservice: Spring generates an HTTP client via {@code @HttpExchange}.
 * Note: Inter-service communication is secured via X-Internal-Service-Key at the transport layer.
 */
@HttpExchange("/internal/orders")
public interface OrderClient {

    @GetExchange("/{id}")
    OrderPaymentDto findOrderForPayment(
            @PathVariable UUID id,
            @RequestParam("userId") UUID userId,
            @RequestParam(value = "isAdmin", defaultValue = "false") boolean isAdmin
    );

    @PostExchange("/{id}/validate-stock")
    void validateStockForPayment(@PathVariable UUID id);

    @PostExchange("/{id}/accept-prepayment")
    void acceptOrderForPrepayment(@PathVariable UUID id);
}
