package com.example.demo.order.service;

import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.model.Order;
import com.example.demo.common.model.User;
import com.example.demo.order.dto.CreateOrderRequest;
import com.example.demo.order.dto.CreateOrderResponse;
import com.example.demo.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_shouldSaveAndReturnResponse() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setName("Test User");
        CustomUserDetails userDetails = new CustomUserDetails(user);

        CreateOrderRequest request = new CreateOrderRequest(
                "Test Receiver", "1234567890", "Test Address", "CASH_ON_DELIVERY", new ArrayList<>()
        );

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            return order;
        });

        // Act
        CreateOrderResponse response = orderService.create(request, "idempotency-key", userDetails);

        // Assert
        assertNotNull(response);
        verify(orderRepository, times(1)).save(any(Order.class));
    }
}
