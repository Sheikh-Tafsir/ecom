package com.example.ecom.order.service;

import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.common.enums.PaymentMethod;
import com.example.ecom.common.model.Order;
import com.example.ecom.common.model.User;
import com.example.ecom.common.service.IdempotencyService;
import com.example.ecom.order.dto.CreateOrderRequest;
import com.example.ecom.order.dto.CreateOrderResponse;
import com.example.ecom.order.repository.OrderRepository;
import com.example.ecom.product.product.service.ProductService;
import com.example.ecom.user.user.service.UserService;
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

    @Mock
    private UserService userService;

    @Mock
    private ProductService productService;

    @Mock
    private IdempotencyService idempotencyService;

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
                new ArrayList<>(), "Test Receiver", "01234567890", "Test Address 12345", PaymentMethod.CASH_ON_DELIVERY
        );

        when(userService.findByIdHelper(1L)).thenReturn(user);
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
