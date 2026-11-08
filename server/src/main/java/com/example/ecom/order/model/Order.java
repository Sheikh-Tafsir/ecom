package com.example.ecom.order.model;

import com.example.ecom.common.model.AuditableEntity;
import com.example.ecom.order.dto.ProductDto;
import com.example.ecom.order.enums.OrderStatus;
import com.example.ecom.order.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order extends AuditableEntity {

    private UUID userId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrderItem> items = new HashSet<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<OrderStatusHistory> statusHistories = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String address;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod = PaymentMethod.CASH_ON_DELIVERY;

    private boolean paid;

    @Column(nullable = false)
    private String idempotencyKey;

    public boolean isCancelledOrRejected() {
        return status == OrderStatus.CANCELLED || status == OrderStatus.REJECTED;
    }

    public void addItem(ProductDto product) {
        OrderItem existingOrderItem = getItem(product.id());

        if (existingOrderItem != null) {
            existingOrderItem.increaseQuantity(product.quantity());
        } else {
            OrderItem item = new OrderItem(product, product.quantity(), this);
            items.add(item);
        }

        calculateTotal();
    }

    private OrderItem getItem(UUID productId) {
        return items.stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    private void calculateTotal() {
        this.totalPrice = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
