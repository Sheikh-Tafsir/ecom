package com.example.demo.order.service;

import com.example.demo.common.dto.CustomUserDetails;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.demo.common.utils.SecurityConstants.HAS_ROLE_ADMIN;
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

    @PreAuthorize(HAS_ROLE_ADMIN)
    public Page<OrderListResponse> findAll(Pageable pageable, OrderStatus status) {
        return orderRepository.findAllByStatus(status, getValidPageable(pageable)).map(OrderListResponse::new);
    }

    public Page<OrderListResponse> findByUser(Long userId, Pageable pageable) {
        return orderRepository.findByUser_Id(userId, getValidPageable(pageable)).map(OrderListResponse::new);
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
            throw new RuntimeException("Cancelled/Rejected order cannot be reopened");
        }

        if (!order.getStatus().canTransitionTo(status)) {
            throw new RuntimeException("Invalid order status transition");
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
