package com.example.ecom.order.service;

import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.common.service.notification.NotificationService;
import com.example.ecom.order.enums.OrderStatus;
import com.example.ecom.order.enums.PaymentMethod;
import com.example.ecom.order.model.Order;
import com.example.ecom.common.client.order.OrderPaymentDto;
import com.example.ecom.common.client.product.ProductClient;
import com.example.ecom.common.client.product.ProductBasicDto;
import com.example.ecom.common.service.IdempotencyService;
import com.example.ecom.order.dto.CreateOrderRequest;
import com.example.ecom.order.dto.CreateOrderResponse;
import com.example.ecom.order.dto.ProductDto;
import com.example.ecom.order.repository.OrderRepository;
import com.example.ecom.common.client.stock.StockClient;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private com.example.ecom.order.repository.OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private com.example.ecom.common.service.MessageService messageService;

    @Mock
    private StockClient stockClient;

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
    @DisplayName("Should save empty order when creating order with no items")
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
    @DisplayName("Should reserve stock for each product when creating order with items")
    void createOrder_withItems_shouldReserveStock() {
        // Arrange
        CustomUserDetails userDetails = createUserDetails();

        ProductBasicDto productSnapshot = new ProductBasicDto(
                PRODUCT_ID, "Test Product", null, new BigDecimal("50.00"), 20
        );

        com.example.ecom.order.dto.CreateOrderItemRequest itemRequest =
                new com.example.ecom.order.dto.CreateOrderItemRequest(PRODUCT_ID, 2);

        CreateOrderRequest request = new CreateOrderRequest(
                java.util.List.of(itemRequest), "Test Receiver", "01234567890", "Test Address 12345", PaymentMethod.CASH_ON_DELIVERY
        );

        when(productClient.findProductById(PRODUCT_ID)).thenReturn(productSnapshot);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(ORDER_ID);
            return order;
        });

        // Act
        CreateOrderResponse response = orderService.create(request, "idempotency-key", userDetails);

        // Assert
        assertNotNull(response);
        verify(productClient, times(1)).reserveStock(PRODUCT_ID, 2);
    }

    @Test
    @DisplayName("Should restore stock when order is cancelled by customer")
    void cancelOrder_shouldRestoreStock() {
        // Arrange
        CustomUserDetails userDetails = createUserDetails();

        Order order = new Order();
        order.setId(ORDER_ID);
        order.setUserId(USER_ID);
        order.setStatus(OrderStatus.PENDING);
        order.addItem(new ProductDto(PRODUCT_ID, "Product", null, new BigDecimal("50.00"), 3));

        when(orderRepository.findById(ORDER_ID)).thenReturn(java.util.Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var response = orderService.cancel(ORDER_ID, userDetails);

        // Assert
        assertNotNull(response);
        verify(productClient, times(1)).releaseStock(PRODUCT_ID, 3);
    }

    @Test
    @DisplayName("Should return OrderPaymentDto when order belongs to calling user")
    void findOrderForPayment_whenOwner_returnsOrderPaymentDto() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setUserId(USER_ID);
        order.setStatus(OrderStatus.PENDING);
        order.addItem(new ProductDto(PRODUCT_ID, "Product", null, new BigDecimal("50.00"), 2));

        when(orderRepository.findDetailsById(ORDER_ID)).thenReturn(java.util.Optional.of(order));

        OrderPaymentDto result = orderService.findOrderForPayment(ORDER_ID, USER_ID, false);

        assertNotNull(result);
        assertEquals(ORDER_ID, result.id());
        assertEquals(USER_ID, result.userId());
        assertEquals("PENDING", result.status());
        assertEquals(new BigDecimal("100.00"), result.totalPrice());
        assertEquals(1, result.items().size());
        assertEquals(PRODUCT_ID, result.items().get(0).productId());
        assertEquals(2, result.items().get(0).quantity());
    }

    @Test
    @DisplayName("Should return OrderPaymentDto when caller is admin even if not order owner")
    void findOrderForPayment_whenAdmin_returnsOrderPaymentDto() {
        UUID otherUserId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setUserId(USER_ID);
        order.setStatus(OrderStatus.PENDING);
        order.addItem(new ProductDto(PRODUCT_ID, "Product", null, new BigDecimal("50.00"), 2));

        when(orderRepository.findDetailsById(ORDER_ID)).thenReturn(java.util.Optional.of(order));

        OrderPaymentDto result = orderService.findOrderForPayment(ORDER_ID, otherUserId, true);

        assertNotNull(result);
        assertEquals(ORDER_ID, result.id());
        assertEquals(USER_ID, result.userId());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when caller is not owner and not admin")
    void findOrderForPayment_whenNotOwnerAndNotAdmin_throwsAccessDeniedException() {
        UUID otherUserId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setUserId(USER_ID);
        order.setStatus(OrderStatus.PENDING);

        when(orderRepository.findDetailsById(ORDER_ID)).thenReturn(java.util.Optional.of(order));

        assertThrows(AccessDeniedException.class,
                () -> orderService.findOrderForPayment(ORDER_ID, otherUserId, false));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when order does not exist during findOrderForPayment")
    void findOrderForPayment_whenOrderNotFound_throwsEntityNotFoundException() {
        when(orderRepository.findDetailsById(ORDER_ID)).thenReturn(java.util.Optional.empty());
        when(messageService.get(eq("error.entity.not.found"), eq("Order"), eq(ORDER_ID)))
                .thenReturn("Order not found: " + ORDER_ID);

        assertThrows(EntityNotFoundException.class,
                () -> orderService.findOrderForPayment(ORDER_ID, USER_ID, false));
    }

    @Test
    @DisplayName("Should validate stock successfully when product has sufficient stock")
    void validateStockForPayment_whenStockAvailable_succeeds() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setUserId(USER_ID);
        order.addItem(new ProductDto(PRODUCT_ID, "Product", null, new BigDecimal("50.00"), 2));

        when(orderRepository.findDetailsById(ORDER_ID)).thenReturn(java.util.Optional.of(order));
        when(productClient.hasStock(PRODUCT_ID, 2)).thenReturn(true);

        assertDoesNotThrow(() -> orderService.validateStockForPayment(ORDER_ID));
        verify(productClient).hasStock(PRODUCT_ID, 2);
    }

    @Test
    @DisplayName("Should throw ValidationException when product has insufficient stock")
    void validateStockForPayment_whenStockInsufficient_throwsValidationException() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setUserId(USER_ID);
        order.addItem(new ProductDto(PRODUCT_ID, "Product", null, new BigDecimal("50.00"), 5));

        when(orderRepository.findDetailsById(ORDER_ID)).thenReturn(java.util.Optional.of(order));
        when(productClient.hasStock(PRODUCT_ID, 5)).thenReturn(false);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> orderService.validateStockForPayment(ORDER_ID));
        assertTrue(ex.getMessage().contains("Insufficient stock"));
    }
}
