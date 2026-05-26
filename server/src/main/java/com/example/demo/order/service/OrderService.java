package com.example.demo.order.service;

import com.example.demo.common.enums.OrderStatus;
import com.example.demo.common.model.Order;
import com.example.demo.common.model.OrderItem;
import com.example.demo.common.model.Product;
import com.example.demo.common.model.User;
import com.example.demo.common.service.MessageService;
import com.example.demo.order.dto.CreateOrderRequest;
import com.example.demo.order.dto.CreateOrderItemRequest;
import com.example.demo.order.dto.OrderResponse;
import com.example.demo.order.repository.OrderRepository;
import com.example.demo.product.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.util.StringUtils.hasText;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    private final ProductService productService;

    private final MessageService messageService;

    public Page<OrderResponse> findAll(Pageable pageable, String status) {
        OrderStatus orderStatus = hasText(status) ? OrderStatus.fromValue(status) : null;
        return orderRepository.findAllByStatus(orderStatus, pageable).map(OrderResponse::new);
    }

    public Page<OrderResponse> findByUser(Long userId, Pageable pageable) {
        return orderRepository.findByUser_Id(userId, pageable).map(OrderResponse::new);
    }

    public OrderResponse findById(Long id) {
        return new OrderResponse(findByIdHelper(id));
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request, User user) {
        Order order = new Order();
        order.setUser(user);

        request.items().forEach(itemRequest -> addProduct(order, itemRequest));

        return new OrderResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus status) {
        Order order = findByIdHelper(id);

        if (isCancellationStatus(order.getStatus()) && !isCancellationStatus(status)) {
            throw new ValidationException("Cancelled order cannot be reopened");
        }

        if (isCancellationStatus(status) && !isCancellationStatus(order.getStatus())) {
            order.getItems()
                    .forEach(item -> productService.increaseStock(item.getProduct(), item.getQuantity()));
        }

        order.setStatus(status);
        return new OrderResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse addItem(Long orderId, CreateOrderItemRequest request) {
        Order order = findByIdHelper(orderId);
        ensureOrderIsModifiable(order);
        addProduct(order, request);

        return new OrderResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse increaseItem(Long orderId, Long productId, int quantity) {
        Order order = findByIdHelper(orderId);
        ensureOrderIsModifiable(order);
        Product product = productService.findEntityById(productId);
        getItem(order, productId);

        productService.decreaseStock(product, quantity);
        order.increaseItem(productId, quantity);

        return new OrderResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse decreaseItem(Long orderId, Long productId, int quantity) {
        Order order = findByIdHelper(orderId);
        ensureOrderIsModifiable(order);
        OrderItem item = getItem(order, productId);

        if (quantity > item.getQuantity()) {
            throw new ValidationException("Quantity is greater than order item quantity");
        }

        productService.increaseStock(item.getProduct(), quantity);
        order.decreaseItem(productId, quantity);

        return new OrderResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse removeItem(Long orderId, Long productId) {
        Order order = findByIdHelper(orderId);
        ensureOrderIsModifiable(order);
        OrderItem item = getItem(order, productId);

        productService.increaseStock(item.getProduct(), item.getQuantity());
        order.removeItem(productId);

        return new OrderResponse(orderRepository.save(order));
    }

    @Transactional
    public void delete(Long id) {
        Order order = findByIdHelper(id);

        if (!isCancellationStatus(order.getStatus())) {
            order.getItems()
                    .forEach(item -> productService.increaseStock(item.getProduct(), item.getQuantity()));
        }

        orderRepository.delete(order);
    }

    private void addProduct(Order order, CreateOrderItemRequest request) {
        Product product = productService.findEntityById(request.productId());

        productService.decreaseStock(product, request.quantity());
        order.addItem(product, request.quantity());
    }

    private Order findByIdHelper(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "Order", id)));
    }

    private OrderItem getItem(Order order, Long productId) {
        return order.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "OrderItem", productId)));
    }

    private boolean isCancellationStatus(OrderStatus status) {
        return status == OrderStatus.CANCELLED || status == OrderStatus.REJECTED;
    }

    private void ensureOrderIsModifiable(Order order) {
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new ValidationException("Only created orders can be modified");
        }
    }
}
