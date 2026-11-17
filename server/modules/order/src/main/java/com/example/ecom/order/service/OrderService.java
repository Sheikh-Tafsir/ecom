package com.example.ecom.order.service;

import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.common.dto.DateRangeDto;
import com.example.ecom.common.enums.NotificationType;
import com.example.ecom.order.enums.OrderStatus;
import com.example.ecom.order.model.Order;
import com.example.ecom.order.model.OrderItem;
import com.example.ecom.order.model.OrderStatusHistory;
import com.example.ecom.common.service.MessageService;
import com.example.ecom.order.dto.*;
import com.example.ecom.order.repository.OrderRepository;
import com.example.ecom.order.dto.CreateOrderResponse;
import com.example.ecom.order.repository.OrderStatusHistoryRepository;
import com.example.ecom.common.client.order.OrderItemPaymentDto;
import com.example.ecom.common.client.order.OrderPaymentDto;
import com.example.ecom.common.client.product.ProductClient;
import com.example.ecom.common.client.product.ProductBasicDto;
import com.example.ecom.common.client.stock.StockClient;
import com.example.ecom.common.event.EventPublisher;
import com.example.ecom.common.event.OrderStockReleaseEvent;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.example.ecom.common.enums.Permission.*;
import static com.example.ecom.common.utils.DateUtils.resolveDates;
import static com.example.ecom.common.utils.SecurityUtil.*;
import static com.example.ecom.common.utils.Utils.getValidPageable;
import static com.example.ecom.common.utils.Utils.isNull;
import static com.example.ecom.common.event.EventConstants.*;

import com.example.ecom.common.service.notification.NotificationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final ProductClient productClient;

    private final MessageService messageService;

    private final OrderRepository orderRepository;

    private final StockClient stockClient;

    private final NotificationService notificationService;

    private final EventPublisher domainEventPublisher;

    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    private final OrderRejectionService orderRejectionService;

    public Page<OrderListResponse> findAll(LocalDate fromDate, LocalDate toDate, OrderStatus status, String productName,
                                           CustomUserDetails userDetails, Pageable pageable) {

        DateRangeDto dateRange = resolveDates(fromDate, toDate);

        List<OrderStatus> statuses = status == null ? null : List.of(status);

        if (hasPermission(List.of(SUPER_ADMIN_ACCESS.getValue(), ADMIN_ACCESS.getValue()), userDetails)) {
            return orderRepository.findAllByStatus(null, statuses, dateRange.fromDate(), dateRange.toDate(), productName,
                    getValidPageable(pageable)).map(OrderListResponse::new);
        }

        if (hasPermission(List.of(DELIVERY_MAN_ACCESS.getValue()), userDetails)) {
            if (statuses == null) {
                return orderRepository.findAllByStatus(null,
                        List.of(OrderStatus.SHIPPED, OrderStatus.DELIVERED, OrderStatus.RETURNED),
                        dateRange.fromDate(), dateRange.toDate(), productName, getValidPageable(pageable)).map(OrderListResponse::new);
            }

            if (status != OrderStatus.SHIPPED && status != OrderStatus.DELIVERED && status != OrderStatus.RETURNED) {
                statuses = List.of(OrderStatus.SHIPPED);
            }

            return orderRepository.findAllByStatus(null, statuses, dateRange.fromDate(), dateRange.toDate(), productName,
                    getValidPageable(pageable)).map(OrderListResponse::new);
        }

        return orderRepository.findAllByStatus(userDetails.getId(), statuses, dateRange.fromDate(), dateRange.toDate(),
                productName, getValidPageable(pageable)).map(OrderListResponse::new);
    }

    // -- Internal methods --

    @Transactional(readOnly = true)
    public OrderPaymentDto findOrderForPayment(UUID id, UUID userId, boolean isAdmin) {
        Order order = orderRepository.findDetailsById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "Order", id)));

        boolean isOwner = userId != null && order.getUserId().equals(userId);
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Access denied");
        }

        List<OrderItemPaymentDto> items = order.getItems().stream()
                .map(item -> new OrderItemPaymentDto(item.getProductId(), item.getQuantity()))
                .toList();

        return new OrderPaymentDto(order.getId(), order.getUserId(), order.getStatus().name(),
                order.getTotalPrice(), order.isPaid(), items);
    }

    public OrderResponse findById(UUID id, CustomUserDetails userDetails) {
        return new OrderResponse(findByIdHelper(id, userDetails));
    }

    public Order findByIdHelper(UUID id, CustomUserDetails userDetails) {
        Order order = orderRepository.findDetailsById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "Order", id)));

        boolean isOwner = userDetails != null && order.getUserId().equals(userDetails.getId());
        boolean isAdmin = userDetails != null && hasPermission(
                List.of(ADMIN_ACCESS.getValue(), SUPER_ADMIN_ACCESS.getValue()), userDetails);

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Access denied");
        }
        return order;
    }

    @Transactional(readOnly = true)
    public void validateStockForPayment(UUID id) {
        Order order = orderRepository.findDetailsById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "Order", id)));

        validateStockForPayment(order);
    }

    public void validateStockForPayment(Order order) {
        order.getItems().forEach(item -> {
            if (!productClient.hasStock(item.getProductId(), item.getQuantity())) {
                throw new ValidationException("Insufficient stock for product: " + item.getProductName());
            }
        });
    }

    @Transactional
    public CreateOrderResponse create(CreateOrderRequest request, String idempotencyKey, CustomUserDetails userDetails) {
        Order order = new Order();
        order.setName(request.name());
        order.setPhone(request.phone());
        order.setAddress(request.address());
        order.setPaymentMethod(request.paymentMethod());
        order.setIdempotencyKey(idempotencyKey);

        if (isNull(order.getName())) {
            order.setName(userDetails.getName());
        }

        order.setUserId(userDetails.getId());
        request.items().forEach(itemRequest -> addProduct(order, itemRequest));

        // Reserve stock at checkout — track reservations for compensation
        List<OrderItem> reserved = new ArrayList<>();
        try {
            for (OrderItem item : order.getItems()) {
                productClient.reserveStock(item.getProductId(), item.getQuantity());
                reserved.add(item);
            }

            Order savedOrder = orderRepository.save(order);
            recordStatusChange(savedOrder, null, OrderStatus.PENDING, userDetails.getId(), "Order created");

            return new CreateOrderResponse(savedOrder.getId(), savedOrder.getTotalPrice());
        } catch (Exception e) {
            // Compensate: release all successfully reserved stock
            compensateStockReservation(reserved);
            throw e;
        }
    }

    @Transactional
    public OrderResponse cancel(UUID id, CustomUserDetails userDetails) {
        Order order = findByIdHelper(id);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Order is not in pending state");
        }

        if (!isOwner(order.getUserId(), userDetails)) {
            throwAccessException(order.getUserId(), userDetails.getId(), "Order", order.getId());
        }

        recordStatusChange(order, OrderStatus.PENDING, OrderStatus.CANCELLED, userDetails.getId(), "Order cancelled by customer");
        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        // Publish stock-release event AFTER transaction commits
        publishStockReleaseEvent(order);

        return new OrderResponse(saved);
    }

    @PreAuthorize("hasAnyAuthority(T(com.example.ecom.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue()," +
            "T(com.example.ecom.common.enums.Permission).DELIVERY_MAN_ACCESS.getValue())")
    @Transactional
    public OrderResponse updateStatus(UUID id, UpdateOrderStatusRequest request, CustomUserDetails userDetails) {
        Order order = findByIdHelper(id);
        OrderStatus status = request.status();
        OrderStatus oldStatus = order.getStatus();
        log.info("Update order: {} from {} to {} by user: {}", id, oldStatus, status, userDetails.getId());

        if (order.isCancelledOrRejected() && !status.isCancellationOrRejection()) {
            throw new IllegalArgumentException("Cancelled/Rejected order cannot be reopened");
        }

        if (!order.getStatus().canTransitionTo(status)) {
            throw new IllegalArgumentException("Invalid order status transition");
        }

        if (hasPermission(List.of(DELIVERY_MAN_ACCESS.getValue()), userDetails)) {
            if (status != OrderStatus.DELIVERED && status != OrderStatus.RETURNED) {
                throw new AccessDeniedException("Delivery man " + userDetails + " can't access order: " + id);
            }

            if (status == OrderStatus.DELIVERED) {
                order.setPaid(true);
            }
        }

        boolean stockConsumed = false;
        try {
            if (status == OrderStatus.ACCEPTED && order.getStatus() != OrderStatus.ACCEPTED) {
                consumeStockForAcceptedOrder(order);
                stockConsumed = true;
            }

            recordStatusChange(order, oldStatus, status, userDetails.getId(), "Status updated to " + status.getValue());
            order.setStatus(status);

            Order saved = orderRepository.save(order);

            // Publish stock-release event AFTER transaction commits (cancel/reject/return)
            if ((status == OrderStatus.CANCELLED || status == OrderStatus.REJECTED || status == OrderStatus.RETURNED)
                    && (oldStatus == OrderStatus.PENDING || oldStatus == OrderStatus.ACCEPTED || oldStatus == OrderStatus.SHIPPED)) {
                publishStockReleaseEvent(order);
            }

            return new OrderResponse(saved);
        } catch (Exception e) {
            if (stockConsumed) {
                log.error("CRITICAL: Stock was consumed for order {} but order update failed: {}. " +
                        "Manual reconciliation required — reservation is NOT released to prevent phantom stock.",
                        id, e.getMessage(), e);
            }
            throw e;
        }
    }

    // -- helpers --
    @Transactional
    public void delete(UUID id) {
        Order order = findByIdHelper(id);
        orderRepository.delete(order);
    }

    @Transactional
    public void acceptOrderForPrepayment(UUID id) {
        Order order = findByIdHelper(id);
        OrderStatus oldStatus = order.getStatus();

        boolean stockConsumed = false;
        try {
            if (order.getStatus() != OrderStatus.ACCEPTED) {
                consumeStockForAcceptedOrder(order);
                stockConsumed = true;
                order.setStatus(OrderStatus.ACCEPTED);
                recordStatusChange(order, oldStatus, OrderStatus.ACCEPTED, order.getUserId(), "Accepted upon payment confirmation");
            }
            order.setPaid(true);
            orderRepository.save(order);
        } catch (Exception e) {
            if (stockConsumed) {
                log.error("CRITICAL: Stock was consumed for prepayment order {} but order save failed: {}. " +
                        "Manual reconciliation required — reservation is NOT released to prevent phantom stock.",
                        id, e.getMessage(), e);
            }
            throw e;
        }
    }

    public void notifyAndRejectPendingOrders() {
        java.time.Instant oneDayAgo = java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS);
        java.time.Instant twoDaysAgo = java.time.Instant.now().minus(2, java.time.temporal.ChronoUnit.DAYS);

        // 1. Notify admins about orders > 1 day old
        List<Order> ordersToNotify = orderRepository.findAllByStatusAndCreatedAtBefore(OrderStatus.PENDING, oneDayAgo);
        if (!ordersToNotify.isEmpty()) {
            String message = "There are " + ordersToNotify.size() + " orders pending for more than 1 day.";
            notificationService.sendToAdmins(message, NotificationType.WARNING);
            log.info("Notified connected admins about {} pending orders", ordersToNotify.size());
        }

        // 2. Reject orders > 2 days old (each in its own transaction via OrderRejectionService)
        List<Order> ordersToReject = orderRepository.findAllByStatusAndCreatedAtBefore(OrderStatus.PENDING, twoDaysAgo);
        ordersToReject.forEach(order -> {
            try {
                orderRejectionService.rejectSingleOrder(order);
            } catch (Exception e) {
                log.error("Failed to auto-reject order {}: {}", order.getId(), e.getMessage(), e);
            }
        });
    }

    private void recordStatusChange(Order order, OrderStatus fromStatus, OrderStatus toStatus, UUID changedById, String comment) {
        OrderStatusHistory history = new OrderStatusHistory(order, fromStatus, toStatus, changedById, comment);
        orderStatusHistoryRepository.save(history);

        if (order.getStatusHistories() != null) {
            order.getStatusHistories().add(history);
        }
    }

    /**
     * Publishes an async stock-release event after the current transaction commits.
     * The product-service consumer will handle the actual stock restoration.
     */
    private void publishStockReleaseEvent(Order order) {
        List<OrderStockReleaseEvent.Item> items = order.getItems().stream()
                .map(item -> new OrderStockReleaseEvent.Item(item.getProductId(), item.getQuantity()))
                .toList();

        domainEventPublisher.publish(
                SOURCE_ORDER,
                ORDER_STOCK_RELEASE,
                new OrderStockReleaseEvent(order.getId(), items)
        );
    }

    /**
     * Compensation: best-effort release of stock that was reserved by a cross-service call.
     * Each release is attempted independently — a failure in one does not block the others.
     * Failures are logged at ERROR level for manual reconciliation.
     */
    private void compensateStockReservation(List<OrderItem> reservedItems) {
        for (OrderItem item : reservedItems) {
            try {
                productClient.releaseStock(item.getProductId(), item.getQuantity());
            } catch (Exception ex) {
                log.error("CRITICAL: Failed to compensate stock reservation for product {} qty {}. " +
                        "Manual reconciliation required.", item.getProductId(), item.getQuantity(), ex);
            }
        }
    }

    private Order findByIdHelper(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "Order", id)));
    }

    private void addProduct(Order order, CreateOrderItemRequest request) {
        ProductBasicDto product = productClient.findProductById(request.productId());

        ProductDto productDto = new ProductDto(product.id(), product.name(), product.firstImage(), product.price(), request.quantity());
        order.addItem(productDto);
    }

    private void consumeStockForAcceptedOrder(Order order) {
        // Sort by product ID to prevent deadlock from non-deterministic lock order
        order.getItems().stream()
            .sorted(Comparator.comparing(OrderItem::getProductId))
            .forEach(item -> stockClient.consume(item.getProductId(), item.getQuantity(), order.getId()));
    }
}
