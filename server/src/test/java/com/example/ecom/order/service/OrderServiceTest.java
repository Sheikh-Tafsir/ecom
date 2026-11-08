package com.example.ecom.order.service;

import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.common.service.notification.NotificationService;
import com.example.ecom.order.enums.OrderStatus;
import com.example.ecom.order.enums.PaymentMethod;
import com.example.ecom.order.model.Order;
import com.example.ecom.product.product.model.Product;
import com.example.ecom.common.service.IdempotencyService;
import com.example.ecom.order.dto.CreateOrderRequest;
import com.example.ecom.order.dto.CreateOrderResponse;
import com.example.ecom.order.dto.ProductDto;
import com.example.ecom.order.repository.OrderRepository;
import com.example.ecom.product.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductService productService;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private com.example.ecom.order.repository.OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private com.example.ecom.common.service.MessageService messageService;

    @Mock
    private com.example.ecom.product.stock.service.StockService stockService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderService orderService;

    private static final UUID USER_ID   = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ORDER_ID  = UUID.fromString("00000000-0000-0000-0000-000000000100");
    private static final UUID PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    private CustomUserDetails createUserDetails() {
        return new CustomUserDetails(USER_ID, "Test User", "test@example.com", true, java.util.List.of());
    }

    @Test
    void createOrder_withNoItems_shouldSaveEmptyOrder() {
        // Arrange
        CustomUserDetails userDetails = createUserDetails();

        CreateOrderRequest request = new CreateOrderRequest(
                new ArrayList<>(), "Test Receiver", "01234567890", "Test Address 12345", PaymentMethod.CASH_ON_DELIVERY
        );

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(ORDER_ID);
            return order;
        });

        // Act
        CreateOrderResponse response = orderService.create(request, "idempotency-key", userDetails);

        // Assert
        assertNotNull(response);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void createOrder_withItems_shouldReserveStock() {
        // Arrange
        CustomUserDetails userDetails = createUserDetails();

        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setPrice(new java.math.BigDecimal("50.00"));
        product.setQuantity(20);

        com.example.ecom.order.dto.CreateOrderItemRequest itemRequest =
                new com.example.ecom.order.dto.CreateOrderItemRequest(PRODUCT_ID, 2);

        CreateOrderRequest request = new CreateOrderRequest(
                java.util.List.of(itemRequest), "Test Receiver", "01234567890", "Test Address 12345", PaymentMethod.CASH_ON_DELIVERY
        );

        when(productService.findByIdHelper(PRODUCT_ID)).thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(ORDER_ID);
            return order;
        });

        // Act
        CreateOrderResponse response = orderService.create(request, "idempotency-key", userDetails);

        // Assert
        assertNotNull(response);
        verify(productService, times(1)).decreaseForOrder(PRODUCT_ID, 2);
    }

    @Test
    void cancelOrder_shouldRestoreStock() {
        // Arrange
        CustomUserDetails userDetails = createUserDetails();

        Order order = new Order();
        order.setId(ORDER_ID);
        order.setUserId(USER_ID);
        order.setStatus(OrderStatus.PENDING);
        order.addItem(new ProductDto(PRODUCT_ID, "Product", null, new java.math.BigDecimal("50.00"), 3));

        when(orderRepository.findById(ORDER_ID)).thenReturn(java.util.Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var response = orderService.cancel(ORDER_ID, userDetails);

        // Assert
        assertNotNull(response);
        verify(productService, times(1)).increaseQuantity(PRODUCT_ID, 3);
    }
}
