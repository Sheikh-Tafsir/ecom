package com.example.ecom.order.service;

import com.example.ecom.order.enums.OrderStatus;
import com.example.ecom.common.event.EventConstants;
import com.example.ecom.common.event.EventPublisher;
import com.example.ecom.common.event.OrderStockReleaseEvent;
import com.example.ecom.order.model.Order;
import com.example.ecom.order.repository.OrderRepository;
import com.example.ecom.order.repository.OrderStatusHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderRejectionServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private EventPublisher domainEventPublisher;

    @InjectMocks
    private OrderRejectionService orderRejectionService;

    @Test
    @DisplayName("rejectSingleOrder should update order to REJECTED, save history, and publish stock release event")
    void rejectSingleOrder_updatesOrderAndPublishesEvent() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PENDING);
        order.setItems(new HashSet<>());
        order.setStatusHistories(new ArrayList<>());

        orderRejectionService.rejectSingleOrder(order);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
        verify(orderRepository).save(order);
        verify(orderStatusHistoryRepository).save(any());

        ArgumentCaptor<OrderStockReleaseEvent> eventCaptor = ArgumentCaptor.forClass(OrderStockReleaseEvent.class);
        verify(domainEventPublisher).publish(
                eq(EventConstants.SOURCE_ORDER),
                eq(EventConstants.ORDER_STOCK_RELEASE),
                eventCaptor.capture()
        );
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(orderId);
    }
}
