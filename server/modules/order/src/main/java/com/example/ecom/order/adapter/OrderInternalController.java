package com.example.ecom.order.adapter;

import com.example.ecom.common.client.order.OrderClient;
import com.example.ecom.common.client.order.OrderPaymentDto;
import com.example.ecom.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Internal REST endpoint for cross-service order operations.
 * Secured by {@code X-Internal-Service-Key} at the Spring Security layer.
 * In monolith mode, these endpoints exist but are unused
 * (callers use {@link OrderClientAdapter} directly).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/orders")
public class OrderInternalController implements OrderClient {

    private final OrderService orderService;

    @GetMapping("/{id}")
    @Override
    public OrderPaymentDto findOrderForPayment(@PathVariable UUID id,
                                               @RequestParam UUID userId,
                                               @RequestParam(defaultValue = "false") boolean isAdmin) {
        return orderService.findOrderForPayment(id, userId, isAdmin);
    }

    @PostMapping("/{id}/validate-stock")
    @Override
    public void validateStockForPayment(@PathVariable UUID id) {
        orderService.validateStockForPayment(id);
    }

    @PostMapping("/{id}/accept-prepayment")
    @Override
    public void acceptOrderForPrepayment(@PathVariable UUID id) {
        orderService.acceptOrderForPrepayment(id);
    }
}
