package com.example.demo.order.service;

import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.enums.OrderStatus;
import com.example.demo.common.helper.CommonHelper;
import com.example.demo.common.model.Order;
import com.example.demo.common.model.OrderItem;
import com.example.demo.common.model.Product;
import com.example.demo.common.model.User;
import com.example.demo.common.service.MessageService;
import com.example.demo.order.dto.CreateOrderRequest;
import com.example.demo.order.dto.CreateOrderItemRequest;
import com.example.demo.order.dto.OrderResponse;
import com.example.demo.order.dto.UpdateOrderStatusRequest;
import com.example.demo.order.repository.OrderRepository;
import com.example.demo.product.service.ProductService;
import com.example.demo.stock.service.StockService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.demo.common.utils.SecurityConstants.HAS_ROLE_ADMIN;
import static com.example.demo.common.utils.SecurityUtil.isAdmin;
import static com.example.demo.common.utils.Utils.getValidPageable;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final ProductService productService;

    private final MessageService messageService;

    private final OrderRepository orderRepository;

    private final StockService stockService;
    private final CommonHelper commonHelper;

    @PreAuthorize(HAS_ROLE_ADMIN)
    public Page<OrderResponse> findAll(Pageable pageable, OrderStatus status) {
        return orderRepository.findAllByStatus(status, getValidPageable(pageable)).map(OrderResponse::new);
    }

    public Page<OrderResponse> findByUser(Long userId, Pageable pageable) {
        return orderRepository.findByUser_Id(userId, getValidPageable(pageable)).map(OrderResponse::new);
    }

    public OrderResponse findById(Long id, CustomUserDetails userDetails) {
        Order order = findByIdHelper(id);
        commonHelper.checkOwnerOrAdmin(order.getUser().getId(), userDetails);

        return new OrderResponse(order);
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request, User user) {
        Order order = new Order();
        order.setUser(user);

        request.items().forEach(itemRequest -> addProduct(order, itemRequest));

        return new OrderResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse updateStatus(Long id, UpdateOrderStatusRequest request, CustomUserDetails userDetails) {
        Order order = findByIdHelper(id);
        OrderStatus status = request.status();

        if (order.isCancelledOrRejected() && !status.isCancellationOrRejection()) {
            throw new RuntimeException("Cancelled/Rejected order cannot be reopened");
        }

        if (!order.getStatus().canTransitionTo(status)) {
            throw new RuntimeException("Invalid order status transition");
        }

        boolean isAdmin = isAdmin(userDetails);

        if (isAdmin) {
            if (!status.canBeSetByAdmin()) {
                throw new AccessDeniedException("Admin with id: "+ id + " attempting to set status with user owner access");
            }
        } else {
            if (!status.canBeSetByUser()) {
                throw new AccessDeniedException("User with id: "+ id + " attempting to set status with admin access");
            }
        }

        if (status == OrderStatus.ACCEPTED) {
            consumeStock(order);
        }

        order.setStatus(status);

        return new OrderResponse(orderRepository.save(order));
    }

//    @Transactional
//    public OrderResponse addItem(Long orderId, CreateOrderItemRequest orderItemRequest) {
//        Order order = findByIdHelper(orderId);
//        ensureOrderIsModifiable(order);
//        addProduct(order, orderItemRequest);
//
//        return new OrderResponse(orderRepository.save(order));
//    }

//    @Transactional
//    public OrderResponse increaseItem(Long orderId, Long productId, int quantity) {
//        Order order = findByIdHelper(orderId);
//        ensureOrderIsModifiable(order);
//
//        getItem(order, productId);
//        order.increaseItem(productId, quantity);
//
//        return new OrderResponse(orderRepository.save(order));
//    }
//
//    @Transactional
//    public OrderResponse decreaseItem(Long orderId, Long productId, int quantity) {
//        Order order = findByIdHelper(orderId);
//        ensureOrderIsModifiable(order);
//        OrderItem item = getItem(order, productId);
//
//        if (quantity > item.getQuantity()) {
//            throw new ValidationException("Quantity is greater than order item quantity");
//        }
//
//        order.decreaseItem(productId, quantity);
//
//        return new OrderResponse(orderRepository.save(order));
//    }
//
//    @Transactional
//    public OrderResponse removeItem(Long orderId, Long productId) {
//        Order order = findByIdHelper(orderId);
//        ensureOrderIsModifiable(order);
//        order.removeItem(productId);
//
//        return new OrderResponse(orderRepository.save(order));
//    }

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

    private void consumeStock(Order order) {
        order.getItems().forEach(this::consumeStock);
    }

    private void consumeStock(OrderItem orderItem) {
        Product product = orderItem.getProduct();
        int quantityToConsume = orderItem.getQuantity();

        productService.consume(product, quantityToConsume);
        stockService.consume(product, quantityToConsume);
    }

//    private void ensureOrderIsModifiable(Order order) {
//        if (order.getStatus() != OrderStatus.CREATED) {
//            throw new RuntimeException("Only created orders can be modified");
//        }
//    }

//    public OrderItem getItem(Order order, Long productId) {
//        return order.getItems().stream()
//                .filter(item -> item.getProduct().getId().equals(productId))
//                .findFirst()
//                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "OrderItem", productId)));
//    }
}
