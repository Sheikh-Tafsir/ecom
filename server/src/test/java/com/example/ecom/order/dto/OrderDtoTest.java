package com.example.ecom.order.dto;

import com.example.ecom.order.enums.OrderStatus;
import com.example.ecom.order.enums.PaymentMethod;
import com.example.ecom.order.model.Order;
import com.example.ecom.order.model.OrderItem;
import com.example.ecom.order.model.OrderStatusHistory;
import com.example.ecom.product.product.model.Product;
import com.example.ecom.product.product.model.ProductImage;
import com.example.ecom.user.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class OrderDtoTest {

    private UUID orderId;
    private UUID userId;
    private UUID productId;
    private Instant now;

    @BeforeEach
    void setUp() {
        orderId = UUID.fromString("01927d6e-097f-7afa-9ac8-859607182930");
        userId = UUID.fromString("01927d6e-1a8f-7bfa-8ac9-960718293041");
        productId = UUID.fromString("01927d6e-2b9f-7cfa-7aca-071829304152");
        now = Instant.parse("2025-08-15T10:30:00Z");
    }

    @Nested
    @DisplayName("CreateOrderRequest Tests")
    class CreateOrderRequestTest {

        @Test
        @DisplayName("Record constructor sets all fields correctly")
        void constructor_validArgs_setsFields() {
            List<CreateOrderItemRequest> items = List.of(
                    new CreateOrderItemRequest(productId, 2)
            );
            CreateOrderRequest request = new CreateOrderRequest(
                    items, "John Doe", "01712345678", "123 Main St, Dhaka", PaymentMethod.BKASH
            );

            assertEquals(items, request.items());
            assertEquals("John Doe", request.name());
            assertEquals("01712345678", request.phone());
            assertEquals("123 Main St, Dhaka", request.address());
            assertEquals(PaymentMethod.BKASH, request.paymentMethod());
        }
    }

    @Nested
    @DisplayName("CreateOrderItemRequest Tests")
    class CreateOrderItemRequestTest {

        @Test
        @DisplayName("Record constructor sets productId and quantity")
        void constructor_validArgs_setsFields() {
            CreateOrderItemRequest request = new CreateOrderItemRequest(productId, 5);

            assertEquals(productId, request.productId());
            assertEquals(5, request.quantity());
        }

        @Test
        @DisplayName("Two equal records are equal")
        void equals_sameFields_returnsTrue() {
            CreateOrderItemRequest r1 = new CreateOrderItemRequest(productId, 3);
            CreateOrderItemRequest r2 = new CreateOrderItemRequest(productId, 3);

            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
        }
    }

    @Nested
    @DisplayName("CreateOrderResponse Tests")
    class CreateOrderResponseTest {

        @Test
        @DisplayName("Record constructor sets id and totalPrice")
        void constructor_validArgs_setsFields() {
            CreateOrderResponse response = new CreateOrderResponse(orderId, new BigDecimal("450.00"));

            assertEquals(orderId, response.id());
            assertEquals(new BigDecimal("450.00"), response.totalPrice());
        }
    }

    @Nested
    @DisplayName("UpdateOrderItemRequest Tests")
    class UpdateOrderItemRequestTest {

        @Test
        @DisplayName("Record constructor sets quantity")
        void constructor_validArgs_setsFields() {
            UpdateOrderItemRequest request = new UpdateOrderItemRequest(10);
            assertEquals(10, request.quantity());
        }
    }

    @Nested
    @DisplayName("UpdateOrderRequest Tests")
    class UpdateOrderRequestTest {

        @Test
        @DisplayName("Record constructor sets items list")
        void constructor_validArgs_setsFields() {
            List<UpdateOrderItemRequest> items = List.of(
                    new UpdateOrderItemRequest(2),
                    new UpdateOrderItemRequest(5)
            );
            UpdateOrderRequest request = new UpdateOrderRequest(items);

            assertEquals(2, request.items().size());
        }
    }

    @Nested
    @DisplayName("UpdateOrderStatusRequest Tests")
    class UpdateOrderStatusRequestTest {

        @Test
        @DisplayName("Record constructor sets status")
        void constructor_validArgs_setsFields() {
            UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.SHIPPED);
            assertEquals(OrderStatus.SHIPPED, request.status());
        }
    }

    @Nested
    @DisplayName("OrderStatusHistoryResponse Tests")
    class OrderStatusHistoryResponseTest {

        @Test
        @DisplayName("Canonical constructor sets all fields")
        void constructor_canonicalArgs_setsFields() {
            UUID historyId = UUID.fromString("01927d6e-3caf-7dfa-6acb-182930415263");
            UUID changedById = UUID.fromString("01927d6e-4dbf-7efa-5acc-293041526374");

            OrderStatusHistoryResponse response = new OrderStatusHistoryResponse(
                    historyId, OrderStatus.PENDING, OrderStatus.ACCEPTED,
                    changedById, "Admin User", "Approved", now
            );

            assertEquals(historyId, response.id());
            assertEquals(OrderStatus.PENDING, response.fromStatus());
            assertEquals(OrderStatus.ACCEPTED, response.toStatus());
            assertEquals(changedById, response.changedByUserId());
            assertEquals("Admin User", response.changedByUserName());
            assertEquals("Approved", response.comment());
            assertEquals(now, response.createdAt());
        }

        @Test
        @DisplayName("Construction from OrderStatusHistory entity with changedBy user")
        void constructor_fromEntityWithUser_setsFields() {
            UUID historyId = UUID.fromString("01927d6e-5ecf-7ffa-4acd-304152637485");
            User adminUser = new User();
            adminUser.setId(UUID.fromString("01927d6e-6fdf-70fa-3ace-415263748596"));
            adminUser.setName("Super Admin");

            Order order = new Order();
            order.setId(orderId);

            OrderStatusHistory history = new OrderStatusHistory();
            history.setId(historyId);
            history.setOrder(order);
            history.setFromStatus(OrderStatus.ACCEPTED);
            history.setToStatus(OrderStatus.SHIPPED);
            history.setChangedBy(adminUser);
            history.setComment("Dispatched via courier");
            history.setCreatedAt(now);

            OrderStatusHistoryResponse response = new OrderStatusHistoryResponse(history);

            assertEquals(historyId, response.id());
            assertEquals(OrderStatus.ACCEPTED, response.fromStatus());
            assertEquals(OrderStatus.SHIPPED, response.toStatus());
            assertEquals(adminUser.getId(), response.changedByUserId());
            assertEquals("Super Admin", response.changedByUserName());
            assertEquals("Dispatched via courier", response.comment());
            assertEquals(now, response.createdAt());
        }

        @Test
        @DisplayName("Construction from OrderStatusHistory entity without changedBy shows System")
        void constructor_fromEntityWithoutUser_showsSystem() {
            OrderStatusHistory history = new OrderStatusHistory();
            history.setId(UUID.fromString("01927d6e-70ef-71fa-2acf-526374859607"));
            history.setFromStatus(OrderStatus.PENDING);
            history.setToStatus(OrderStatus.REJECTED);
            history.setChangedBy(null);
            history.setComment("Auto-rejected after timeout");
            history.setCreatedAt(now);

            OrderStatusHistoryResponse response = new OrderStatusHistoryResponse(history);

            assertNull(response.changedByUserId());
            assertEquals("System", response.changedByUserName());
        }
    }

    @Nested
    @DisplayName("OrderItemResponse Tests")
    class OrderItemResponseTest {

        @Test
        @DisplayName("Construction from OrderItem entity maps all fields")
        void constructor_fromEntity_setsAllFields() {
            UUID itemId = UUID.fromString("01927d6e-81ff-72fa-1ad0-637485960718");
            Product product = new Product();
            product.setId(productId);
            product.setName("Wireless Mouse");
            product.setPrice(new BigDecimal("49.99"));

            ProductImage image = new ProductImage();
            image.setImage("https://cdn.example.com/mouse.jpg");
            product.addImage(image);

            Order order = new Order();
            order.setId(orderId);

            OrderItem item = new OrderItem();
            item.setId(itemId);
            item.setProduct(product);
            item.setOrder(order);
            item.setQuantity(3);
            item.setUnitPrice(new BigDecimal("49.99"));

            OrderItemResponse response = new OrderItemResponse(item);

            assertEquals(itemId, response.getId());
            assertEquals(orderId, response.getOrderId());
            assertEquals(productId, response.getProductId());
            assertEquals("Wireless Mouse", response.getProductName());
            assertEquals("https://cdn.example.com/mouse.jpg", response.getProductImage());
            assertEquals(new BigDecimal("49.99"), response.getProductPrice());
            assertEquals(3, response.getQuantity());
            assertEquals(new BigDecimal("149.97"), response.getSubtotal());
        }

        @Test
        @DisplayName("Construction from OrderItem with no images sets productImage to null")
        void constructor_fromEntityNoImages_productImageNull() {
            Product product = new Product();
            product.setId(productId);
            product.setName("Keyboard");
            product.setPrice(new BigDecimal("79.99"));

            Order order = new Order();
            order.setId(orderId);

            OrderItem item = new OrderItem();
            item.setId(UUID.fromString("01927d6e-920f-73fa-0ad1-748596071829"));
            item.setProduct(product);
            item.setOrder(order);
            item.setQuantity(1);
            item.setUnitPrice(new BigDecimal("79.99"));

            OrderItemResponse response = new OrderItemResponse(item);

            assertNull(response.getProductImage());
        }

        @Test
        @DisplayName("No-args constructor creates empty response")
        void noArgsConstructor_createsEmptyResponse() {
            OrderItemResponse response = new OrderItemResponse();
            assertNull(response.getId());
            assertNull(response.getOrderId());
            assertNull(response.getProductId());
        }
    }

    @Nested
    @DisplayName("OrderListResponse Tests")
    class OrderListResponseTest {

        @Test
        @DisplayName("Construction from Order entity with user maps all fields")
        void constructor_fromEntityWithUser_setsAllFields() {
            User user = new User();
            user.setId(userId);
            user.setName("Bob Customer");

            Order order = new Order();
            order.setId(orderId);
            order.setUser(user);
            order.setTotalPrice(new BigDecimal("750.00"));
            order.setStatus(OrderStatus.SHIPPED);
            order.setPaymentMethod(PaymentMethod.BKASH);
            order.setPaid(true);
            order.setCreatedAt(now);
            order.setUpdatedAt(now);

            OrderListResponse response = new OrderListResponse(order);

            assertEquals(orderId, response.getId());
            assertEquals(userId, response.getUserId());
            assertEquals("Bob Customer", response.getUserName());
            assertEquals(new BigDecimal("750.00"), response.getTotalPrice());
            assertEquals(OrderStatus.SHIPPED, response.getStatus());
            assertEquals(PaymentMethod.BKASH, response.getPaymentMethod());
            assertTrue(response.isPaid());
            assertEquals(now, response.getCreatedAt());
            assertEquals(now, response.getUpdatedAt());
        }

        @Test
        @DisplayName("Construction from Order entity without user sets null userId and userName")
        void constructor_fromEntityWithoutUser_nullUserFields() {
            Order order = new Order();
            order.setId(orderId);
            order.setUser(null);
            order.setTotalPrice(new BigDecimal("100.00"));
            order.setStatus(OrderStatus.PENDING);

            OrderListResponse response = new OrderListResponse(order);

            assertNull(response.getUserId());
            assertNull(response.getUserName());
        }
    }

    @Nested
    @DisplayName("OrderResponse Tests")
    class OrderResponseTest {

        @Test
        @DisplayName("Construction from Order entity with items and histories maps all fields")
        void constructor_fromEntityComplete_setsAllFields() {
            User user = new User();
            user.setId(userId);
            user.setName("Charlie Buyer");

            Product product = new Product();
            product.setId(productId);
            product.setName("Monitor");
            product.setPrice(new BigDecimal("500.00"));

            Order order = new Order();
            order.setId(orderId);
            order.setUser(user);
            order.setName("Charlie Buyer");
            order.setPhone("01798765432");
            order.setAddress("456 Oak Ave, Chittagong");
            order.setStatus(OrderStatus.DELIVERED);
            order.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
            order.setPaid(false);
            order.setCreatedAt(now);
            order.setUpdatedAt(now);
            order.setIdempotencyKey("idem-key-test");
            order.addItem(product, 1);

            OrderStatusHistory history = new OrderStatusHistory();
            history.setId(UUID.fromString("01927d6e-a31f-74fa-9ad2-859607182930"));
            history.setFromStatus(OrderStatus.PENDING);
            history.setToStatus(OrderStatus.DELIVERED);
            history.setChangedBy(null);
            history.setCreatedAt(now);
            order.getStatusHistories().add(history);

            OrderResponse response = new OrderResponse(order);

            assertEquals(orderId, response.getId());
            assertEquals(userId, response.getUserId());
            assertEquals("Charlie Buyer", response.getUserName());
            assertEquals(new BigDecimal("500.00"), response.getTotalPrice());
            assertEquals(OrderStatus.DELIVERED, response.getStatus());
            assertEquals("Charlie Buyer", response.getName());
            assertEquals("456 Oak Ave, Chittagong", response.getAddress());
            assertEquals("01798765432", response.getPhone());
            assertEquals(1, response.getItems().size());
            assertEquals(1, response.getStatusHistories().size());
            assertEquals(PaymentMethod.CASH_ON_DELIVERY, response.getPaymentMethod());
            assertFalse(response.isPaid());
        }

        @Test
        @DisplayName("Construction from Order entity without user and without histories")
        void constructor_fromEntityMinimal_handlesNulls() {
            Order order = new Order();
            order.setId(orderId);
            order.setUser(null);
            order.setStatusHistories(null);
            order.setName("Guest");
            order.setPhone("01700000000");
            order.setAddress("Unknown");
            order.setIdempotencyKey("key");

            OrderResponse response = new OrderResponse(order);

            assertNull(response.getUserId());
            assertNull(response.getUserName());
            assertNull(response.getStatusHistories());
        }
    }
}
