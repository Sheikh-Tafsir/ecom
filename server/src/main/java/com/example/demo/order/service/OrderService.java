package com.example.demo.order.service;

import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.dto.DateRangeDto;
import com.example.demo.common.enums.OrderStatus;
import com.example.demo.common.helper.CommonHelper;
import com.example.demo.common.model.*;
import com.example.demo.common.service.MessageService;
import com.example.demo.order.dto.*;
import com.example.demo.order.repository.OrderRepository;
import com.example.demo.product.service.ProductService;
import com.example.demo.stock.service.StockService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.example.demo.common.utils.DateUtils.resolveDates;
import static com.example.demo.common.utils.SecurityUtil.isAdmin;
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

    private final ModelMapper modelMapper;

    private final CommonHelper commonHelper;

    public Page<OrderListResponse> findAll(LocalDateTime fromDate, LocalDateTime toDate, OrderStatus status, CustomUserDetails userDetails, Pageable pageable) {
        DateRangeDto dateRange = resolveDates(fromDate, toDate);

        if (isAdmin(userDetails)) {
            return orderRepository.findAllByStatus(status, dateRange.fromDate(), dateRange.toDate(), getValidPageable(pageable)).map(OrderListResponse::new);
        }

        return orderRepository.findAllByUser_Id(userDetails.getId(), dateRange.fromDate(), dateRange.toDate(), getValidPageable(pageable)).map(OrderListResponse::new);
    }

    public OrderResponse findById(Long id, CustomUserDetails userDetails) {
        Order order = orderRepository.findDetailsById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "Order", id)));

        commonHelper.checkOwnerOrAdmin(order.getUser().getId(), userDetails);

        return new OrderResponse(order);
    }

    @Transactional
    public long create(CreateOrderRequest request, CustomUserDetails userDetails) {
        Order order = modelMapper.map(request, Order.class);
        if (isNull(order.getName())) {
            order.setName(userDetails.user().getName());
        }

        order.setUser(userDetails.user());
        request.items().forEach(itemRequest -> addProduct(order, itemRequest));

        orderRepository.save(order);

        return order.getId();
    }

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

        boolean isAdmin = isAdmin(userDetails);
        Long userId = userDetails.getId();

        if (isAdmin) {
            if (!status.canBeSetByAdmin()) {
                throw new AccessDeniedException("Admin with id: "+ userId + " attempting to set status with user owner access");
            }
        } else {
            if (!status.canBeSetByUser()) {
                throw new AccessDeniedException("User with id: "+ userId + " attempting to set status with admin access");
            }
        }

        if (status == OrderStatus.ACCEPTED) {
            acceptOrder(order);
        }

        order.setStatus(status);

        return new OrderResponse(orderRepository.save(order));
    }

    @Transactional
    public void delete(Long id) {
        Order order = findByIdHelper(id);
        orderRepository.delete(order);
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
        stockService.consume(product, quantityToConsume);
    }
}
