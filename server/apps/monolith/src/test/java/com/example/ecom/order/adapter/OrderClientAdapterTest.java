package com.example.ecom.order.adapter;

import com.example.ecom.common.client.order.OrderPaymentDto;
import com.example.ecom.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderClientAdapterTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderClientAdapter orderClientAdapter;

    @Test
    @DisplayName("findOrderForPayment should delegate to orderService.findOrderForPayment")
    void findOrderForPayment_delegatesToOrderService() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        boolean isAdmin = false;
        OrderPaymentDto expectedDto = new OrderPaymentDto(
                orderId,
                userId,
                "PENDING",
                new BigDecimal("100.00"),
                false,
                List.of()
        );

        when(orderService.findOrderForPayment(orderId, userId, isAdmin)).thenReturn(expectedDto);

        OrderPaymentDto result = orderClientAdapter.findOrderForPayment(orderId, userId, isAdmin);

        assertEquals(expectedDto, result);
        verify(orderService).findOrderForPayment(orderId, userId, isAdmin);
    }

    @Test
    @DisplayName("validateStockForPayment should delegate to orderService.validateStockForPayment")
    void validateStockForPayment_delegatesToOrderService() {
        UUID orderId = UUID.randomUUID();

        orderClientAdapter.validateStockForPayment(orderId);

        verify(orderService).validateStockForPayment(orderId);
    }

    @Test
    @DisplayName("acceptOrderForPrepayment should delegate to orderService.acceptOrderForPrepayment")
    void acceptOrderForPrepayment_delegatesToOrderService() {
        UUID orderId = UUID.randomUUID();

        orderClientAdapter.acceptOrderForPrepayment(orderId);

        verify(orderService).acceptOrderForPrepayment(orderId);
    }
}
