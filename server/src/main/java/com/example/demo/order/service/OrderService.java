package com.example.demo.order.service;

import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.dto.DateRangeDto;
import com.example.demo.common.enums.NotificationType;
import com.example.demo.common.enums.OrderStatus;
import com.example.demo.common.model.*;
import com.example.demo.common.service.IdempotencyService;
import com.example.demo.common.service.MessageService;
import com.example.demo.notification.dto.NotificationResponse;
import com.example.demo.order.dto.*;
import com.example.demo.order.repository.OrderRepository;
import com.example.demo.order.dto.CreateOrderResponse;
import com.example.demo.product.service.ProductService;
import com.example.demo.stock.service.StockService;
import com.example.demo.user.service.UserService;
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

import static com.example.demo.common.enums.Permission.*;
import static com.example.demo.common.utils.DateUtils.resolveDates;
import static com.example.demo.common.utils.SecurityUtil.*;
import static com.example.demo.common.utils.Utils.getValidPageable;
import static com.example.demo.common.utils.Utils.isNull;

import com.example.demo.notification.service.NotificationService;
import java.time.LocalDateTime;

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

    public Page<OrderListResponse> findAll(LocalDate fromDate, LocalDate toDate, OrderStatus status, CustomUserDetails userDetails, Pageable pageable) {
        DateRangeDto dateRange = resolveDates(fromDate, toDate);

        List<OrderStatus> statuses = status == null ? null : List.of(status);

        if (hasPermission(List.of(SUPER_ADMIN_ACCESS.getValue(), ADMIN_ACCESS.getValue()), userDetails)) {
            return orderRepository.findAllByStatus(null, statuses, dateRange.fromDate(), dateRange.toDate(), getValidPageable(pageable)).map(OrderListResponse::new);
        }

        if (hasPermission(List.of(DELIVERY_MAN_ACCESS.getValue()), userDetails)) {
            if (statuses == null) {
                return orderRepository.findAllByStatus(null,
                        List.of(OrderStatus.SHIPPED, OrderStatus.DELIVERED, OrderStatus.RETURNED),
                        dateRange.fromDate(), dateRange.toDate(), getValidPageable(pageable)).map(OrderListResponse::new);
            }

            if (status != OrderStatus.SHIPPED && status != OrderStatus.DELIVERED && status != OrderStatus.RETURNED) {
               statuses = List.of(OrderStatus.SHIPPED);
            }

            return orderRepository.findAllByStatus(null, statuses, dateRange.fromDate(), dateRange.toDate(), getValidPageable(pageable)).map(OrderListResponse::new);
        }

        return orderRepository.findAllByStatus(userDetails.getId(), statuses, dateRange.fromDate(), dateRange.toDate(), getValidPageable(pageable)).map(OrderListResponse::new);
    }

    @PostAuthorize("""
            returnObject.userId == authentication.principal.id ||
            hasAnyAuthority(T(com.example.demo.common.enums.Permission).ADMIN_ACCESS.getValue(),
            T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())
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

        orderRepository.save(order);

        CreateOrderResponse response = new CreateOrderResponse(order.getId(), order.getTotalPrice());

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        idempotencyService.save(idempotencyKey, request, response);
                    }
                }
        );

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

        order.setStatus(OrderStatus.CANCELLED);

        return new OrderResponse(orderRepository.save(order));
    }

    @PreAuthorize("hasAnyAuthority(T(com.example.demo.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue()," +
            "T(com.example.demo.common.enums.Permission).DELIVERY_MAN_ACCESS.getValue())")
    @Transactional
    public OrderResponse updateStatus(Long id, UpdateOrderStatusRequest request, CustomUserDetails userDetails) {
        Order order = findByIdHelper(id);
        OrderStatus status = request.status();
        log.info("Update order: {} from {} to {} by user: {}", id, order.getStatus(), status, userDetails.getId());

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

        if (status == OrderStatus.ACCEPTED) {
            acceptOrder(order);
        }

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
        acceptOrder(order);
        order.setStatus(OrderStatus.ACCEPTED);
        order.setPaid(true);
        orderRepository.save(order);
    }

    public void notifyAndRejectPendingOrders() {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);

        // 1. Notify admins about orders > 1 days old via SSE
        List<Order> ordersToNotify = orderRepository.findAllByStatusAndCreatedAtBefore(OrderStatus.PENDING, oneDayAgo);
        if (!ordersToNotify.isEmpty()) {
            String message = "There are " + ordersToNotify.size() + " orders pending for more than 2 days.";
            notificationService.sendToAdmins(new NotificationResponse(NotificationType.WARNING, message));
            log.info("Notified connected admins about {} pending orders via SSE", ordersToNotify.size());
        }

        // 2. Reject orders > 2 days old
        List<Order> ordersToReject = orderRepository.findAllByStatusAndCreatedAtBefore(OrderStatus.PENDING, twoDaysAgo);
        ordersToReject.forEach(order -> {
            log.info("Rejecting order {} due to inactivity (more than 3 days old)", order.getId());
            order.setStatus(OrderStatus.REJECTED);
            orderRepository.save(order);
        });
    }

//    @Transactional
//    public void revertOrder(long id) {
//        Order order = findByIdHelper(id);
//        order.getItems().forEach(item -> productService.increaseQuantity(item.getProduct(), item.getQuantity()));
//        order.setStatus(OrderStatus.PENDING);
//        order.setPaid(false);
//        orderRepository.save(order);
//    }

    private Order findByIdHelper(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "Order", id)));
    }

    private void addProduct(Order order, CreateOrderItemRequest request) {
        Product product = productService.findByIdHelper(request.productId());
        order.addItem(product, request.quantity());
    }

    private void acceptOrder(Order order) {
        order.getItems().forEach(this::consumeProductAndStock);
    }

    private void consumeProductAndStock(OrderItem orderItem) {
        Product product = orderItem.getProduct();
        int quantityToConsume = orderItem.getQuantity();

        productService.decreaseForOrder(product, quantityToConsume);
        stockService.consume(product, quantityToConsume, orderItem.getOrder());
    }
}
