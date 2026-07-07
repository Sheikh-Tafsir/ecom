package com.example.demo.order.service;

import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.dto.DateRangeDto;
import com.example.demo.common.enums.OrderStatus;
import com.example.demo.common.model.*;
import com.example.demo.common.service.IdempotencyService;
import com.example.demo.common.service.MessageService;
import com.example.demo.order.dto.*;
import com.example.demo.order.repository.OrderRepository;
import com.example.demo.order.dto.CreateOrderResponse;
import com.example.demo.product.service.ProductService;
import com.example.demo.stock.service.StockService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.demo.common.enums.Permission.ADMIN_ACCESS;
import static com.example.demo.common.enums.Permission.SUPER_ADMIN_ACCESS;
import static com.example.demo.common.utils.DateUtils.resolveDates;
import static com.example.demo.common.utils.SecurityUtil.*;
import static com.example.demo.common.utils.Utils.getValidPageable;
import static com.example.demo.common.utils.Utils.isNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final ProductService productService;

    private final MessageService messageService;

    private final OrderRepository orderRepository;

    private final StockService stockService;

    private final IdempotencyService idempotencyService;

    public Page<OrderListResponse> findAll(LocalDateTime fromDate, LocalDateTime toDate, OrderStatus status, CustomUserDetails userDetails, Pageable pageable) {
        DateRangeDto dateRange = resolveDates(fromDate, toDate);

        if (hasPermission(List.of(SUPER_ADMIN_ACCESS.getValue(), ADMIN_ACCESS.getValue()), userDetails)) {
            return orderRepository.findAllByStatus(status, dateRange.fromDate(), dateRange.toDate(), getValidPageable(pageable)).map(OrderListResponse::new);
        }

        return orderRepository.findAllByUser_Id(userDetails.getId(), dateRange.fromDate(), dateRange.toDate(), getValidPageable(pageable)).map(OrderListResponse::new);
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

        if (isNull(order.getName())) {
            order.setName(userDetails.user().getName());
        }

        order.setUser(userDetails.user());
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
            "T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Transactional
    public OrderResponse updateStatus(Long id, UpdateOrderStatusRequest request, CustomUserDetails userDetails) {
        Order order = findByIdHelper(id);
        OrderStatus status = request.status();
        log.info("Updating order status for id: {} from {} to {} by user: {}", id, order.getStatus(), status, userDetails.getEmail());

        if (order.isCancelledOrRejected() && !status.isCancellationOrRejection()) {
            throw new IllegalArgumentException("Cancelled/Rejected order cannot be reopened");
        }

        if (!order.getStatus().canTransitionTo(status)) {
            throw new IllegalArgumentException("Invalid order status transition");
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
    public void acceptOrder(long id) {
        Order order = findByIdHelper(id);
        acceptOrder(order);
        order.setStatus(OrderStatus.ACCEPTED);
        orderRepository.save(order);
    }

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
