package com.example.ecom.order.adapter;

import com.example.ecom.common.client.order.OrderClient;
import com.example.ecom.common.client.order.OrderPaymentDto;
import com.example.ecom.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Monolith adapter: implements {@link OrderClient} by delegating to {@link OrderService}.
 * On microservice extraction, this class is removed and replaced by an HTTP proxy.
 */
@Service
@RequiredArgsConstructor
public class OrderClientAdapter implements OrderClient {

    private final OrderService orderService;

    @Override
    public OrderPaymentDto findOrderForPayment(UUID id, UUID userId, boolean isAdmin) {
        return orderService.findOrderForPayment(id, userId, isAdmin);
    }

    @Override
    public void validateStockForPayment(UUID id) {
        orderService.validateStockForPayment(id);
    }

    @Override
    public void acceptOrderForPrepayment(UUID id) {
        orderService.acceptOrderForPrepayment(id);
    }
}
