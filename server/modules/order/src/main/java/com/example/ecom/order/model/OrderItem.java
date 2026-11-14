package com.example.ecom.order.model;

import com.example.ecom.order.dto.ProductDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.ecom.common.annotation.UuidV7;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @UuidV7
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "product_image")
    private String productImage;

    private int quantity;

    private BigDecimal unitPrice;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    public OrderItem(ProductDto product, int quantity, Order order) {
        this.productId = product.id();
        this.productName = product.name();
        this.productImage = product.image();
        this.quantity = quantity;
        this.order = order;
        this.unitPrice = product.price();
    }

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public void increaseQuantity(int qty) {
        this.quantity += qty;
    }

    public void decreaseQuantity(int qty) {
        this.quantity -= qty;
    }
}
