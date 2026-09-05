package com.example.ecom.order.service;

import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.common.dto.DateRangeDto;
import com.example.ecom.common.enums.NotificationType;
import com.example.ecom.common.enums.OrderStatus;
import com.example.ecom.common.model.*;
import com.example.ecom.common.service.IdempotencyService;
import com.example.ecom.common.service.MessageService;
import com.example.ecom.notification.dto.NotificationResponse;
import com.example.ecom.order.dto.*;
import com.example.ecom.order.repository.OrderRepository;
import com.example.ecom.order.dto.CreateOrderResponse;
import com.example.ecom.order.repository.OrderStatusHistoryRepository;
import com.example.ecom.product.product.service.ProductService;
import com.example.ecom.product.stock.service.StockService;
import com.example.ecom.user.user.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.List;

import static com.example.ecom.common.enums.Permission.*;
import static com.example.ecom.common.utils.DateUtils.resolveDates;
import static com.example.ecom.common.utils.SecurityUtil.*;
import static com.example.ecom.common.utils.Utils.getValidPageable;
import static com.example.ecom.common.utils.Utils.isNull;

import com.example.ecom.notification.service.NotificationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final ProductService productService;

    private final MessageService messageService;

    private final OrderRepository orderRepository;

    private final StockService stockService;

    private final IdempotencyService idempotencyService;

    private final UserService userService;

    private final NotificationService notificationService;

    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

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

    @PostAuthorize("""
            (returnObject.userId != null && returnObject.userId == authentication.principal.id) ||
            hasAnyAuthority(T(com.example.ecom.common.enums.Permission).ADMIN_ACCESS.getValue(),
            T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue(),
            T(com.example.ecom.common.enums.Permission).DELIVERY_MAN_ACCESS.getValue())
            """)
    public OrderResponse findById(Long id) {
        Order order = orderRepository.findDetailsById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "Order", id)));

        return new OrderResponse(order);
    }

    @Transactional
    public CreateOrderResponse create(CreateOrderRequest request, String idempotencyKey, CustomUserDetails userDetails) {
        Object cachedResponse = idempotencyService.getCachedResponse(idempotencyKey, request);
        if (cachedResponse != null) {
            return (CreateOrderResponse) cachedResponse;
        }

        Order order = new Order();
        order.setName(request.name());
        order.setPhone(request.phone());
        order.setAddress(request.address());
        order.setPaymentMethod(request.paymentMethod());

        User user = userService.findByIdHelper(userDetails.getId());
        if (isNull(order.getName())) {
            order.setName(user.getName());
        }

        order.setUser(user);
        request.items().forEach(itemRequest -> addProduct(order, itemRequest));

        // Reserve stock at checkout
        order.getItems().forEach(item -> productService.decreaseForOrder(item.getProduct(), item.getQuantity()));

        Order savedOrder = orderRepository.save(order);
        recordStatusChange(savedOrder, null, OrderStatus.PENDING, user, "Order created");

        CreateOrderResponse response = new CreateOrderResponse(savedOrder.getId(), savedOrder.getTotalPrice());

        if (idempotencyKey != null) {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                idempotencyService.save(idempotencyKey, request, response);
                            }
                        }
                );
            } else {
                idempotencyService.save(idempotencyKey, request, response);
            }
        }

        return response;
    }

    @Transactional
    public OrderResponse cancel(Long id, CustomUserDetails userDetails) {
        Order order = findByIdHelper(id);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Order is not in pending state");
        }

        if (!isOwner(order.getUser().getId(), userDetails)) {
            throwAccessException(order.getUser().getId(), userDetails.getId(), "Order", order.getId());
        }

        restoreOrderStock(order);

        User actor = userDetails != null ? userService.findByIdHelper(userDetails.getId()) : order.getUser();
        recordStatusChange(order, OrderStatus.PENDING, OrderStatus.CANCELLED, actor, "Order cancelled by customer");
        order.setStatus(OrderStatus.CANCELLED);

        return new OrderResponse(orderRepository.save(order));
    }

    @PreAuthorize("hasAnyAuthority(T(com.example.ecom.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue()," +
            "T(com.example.ecom.common.enums.Permission).DELIVERY_MAN_ACCESS.getValue())")
    @Transactional
    public OrderResponse updateStatus(Long id, UpdateOrderStatusRequest request, CustomUserDetails userDetails) {
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

        if (status == OrderStatus.ACCEPTED && order.getStatus() != OrderStatus.ACCEPTED) {
            consumeStockForAcceptedOrder(order);
        } else if ((status == OrderStatus.CANCELLED || status == OrderStatus.REJECTED || status == OrderStatus.RETURNED)
                && (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.ACCEPTED || order.getStatus() == OrderStatus.SHIPPED)) {
            restoreOrderStock(order);
        }

        User actor = userDetails != null ? userService.findByIdHelper(userDetails.getId()) : null;
        recordStatusChange(order, oldStatus, status, actor, "Status updated to " + status.getValue());
        order.setStatus(status);

        return new OrderResponse(orderRepository.save(order));
    }

    // -- helpers --
    @Transactional
    public void delete(Long id) {
        Order order = findByIdHelper(id);
        orderRepository.delete(order);
    }

    @Transactional
    public void acceptOrderForPrepayment(long id) {
        Order order = findByIdHelper(id);
        OrderStatus oldStatus = order.getStatus();
        if (order.getStatus() != OrderStatus.ACCEPTED) {
            consumeStockForAcceptedOrder(order);
            order.setStatus(OrderStatus.ACCEPTED);
            recordStatusChange(order, oldStatus, OrderStatus.ACCEPTED, order.getUser(), "Accepted upon payment confirmation");
        }
        order.setPaid(true);
        orderRepository.save(order);
    }

    public void notifyAndRejectPendingOrders() {
        java.time.Instant oneDayAgo = java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS);
        java.time.Instant twoDaysAgo = java.time.Instant.now().minus(2, java.time.temporal.ChronoUnit.DAYS);

        // 1. Notify admins about orders > 1 days old via SSE
        List<Order> ordersToNotify = orderRepository.findAllByStatusAndCreatedAtBefore(OrderStatus.PENDING, oneDayAgo);
        if (!ordersToNotify.isEmpty()) {
            String message = "There are " + ordersToNotify.size() + " orders pending for more than 1 day.";
            notificationService.sendToAdmins(new NotificationResponse(NotificationType.WARNING, message));
            log.info("Notified connected admins about {} pending orders via SSE", ordersToNotify.size());
        }

        // 2. Reject orders > 2 days old
        List<Order> ordersToReject = orderRepository.findAllByStatusAndCreatedAtBefore(OrderStatus.PENDING, twoDaysAgo);
        ordersToReject.forEach(order -> {
            log.info("Rejecting order {} due to inactivity (more than 2 days old)", order.getId());
            restoreOrderStock(order);
            recordStatusChange(order, OrderStatus.PENDING, OrderStatus.REJECTED, null, "Auto-rejected due to inactivity (>2 days)");
            order.setStatus(OrderStatus.REJECTED);
            orderRepository.save(order);
        });
    }

    private void recordStatusChange(Order order, OrderStatus fromStatus, OrderStatus toStatus, User user, String comment) {
        com.example.ecom.common.model.OrderStatusHistory history =
                new com.example.ecom.common.model.OrderStatusHistory(order, fromStatus, toStatus, user, comment);
        orderStatusHistoryRepository.save(history);
        if (order.getStatusHistories() != null) {
            order.getStatusHistories().add(history);
        }
    }

    private void restoreOrderStock(Order order) {
        order.getItems().forEach(item -> productService.increaseQuantity(item.getProduct(), item.getQuantity()));
    }

    private Order findByIdHelper(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "Order", id)));
    }

    private void addProduct(Order order, CreateOrderItemRequest request) {
        Product product = productService.findByIdHelper(request.productId());
        order.addItem(product, request.quantity());
    }

    private void consumeStockForAcceptedOrder(Order order) {
        order.getItems().forEach(item -> stockService.consume(item.getProduct(), item.getQuantity(), item.getOrder()));
    }
}
