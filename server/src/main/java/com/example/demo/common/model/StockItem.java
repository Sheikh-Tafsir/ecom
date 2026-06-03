package com.example.demo.common.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "stock_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "stock_id")
    private Stock stock;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private BigDecimal purchasedPrice = BigDecimal.ZERO;

    private int remaining;

    public StockItem(Stock stock, Product product, int quantity, BigDecimal purchasedPrice) {
        this.stock = stock;
        this.product = product;
        this.quantity = quantity;
        this.purchasedPrice = purchasedPrice;
        this.remaining = quantity;
    }

    public BigDecimal getSubtotal() {
        return purchasedPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
