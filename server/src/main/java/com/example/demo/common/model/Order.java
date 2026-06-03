package com.example.demo.common.model;

import com.example.demo.common.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrderItem> items = new HashSet<>();

    @Column(nullable = false)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.CREATED;

    public boolean isCancelledOrRejected() {
        return status == OrderStatus.CANCELLED || status == OrderStatus.REJECTED;
    }

    public void addItem(Product product, int quantity) {
        OrderItem existingOrderItem = getItem(product.getId());

        if (existingOrderItem != null) {
            existingOrderItem.increaseQuantity(quantity);
        } else {
            OrderItem item = new OrderItem(product, quantity, this);
            items.add(item);
        }

        calculateTotal();
    }

    public void removeItem(Long productId) {
        items.removeIf(i -> i.getProduct().getId().equals(productId));
        calculateTotal();
    }

    public void increaseItem(Long productId, int qty) {
        OrderItem item = getItem(productId);
        item.increaseQuantity(qty);
        calculateTotal();
    }

    public void decreaseItem(Long productId, int qty) {
        OrderItem item = getItem(productId);
        item.decreaseQuantity(qty);

        if (item.getQuantity() <= 0) {
            removeItem(productId);
        }

        calculateTotal();
    }

    private OrderItem getItem(Long productId) {
        return items.stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    private void calculateTotal() {
        this.totalPrice = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
