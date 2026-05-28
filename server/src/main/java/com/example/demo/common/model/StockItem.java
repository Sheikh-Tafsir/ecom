package com.example.demo.common.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
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
    private BigDecimal cost = BigDecimal.ZERO;

    private int remaining;

    public StockItem(Stock stock, Product product, int quantity, BigDecimal cost) {
        this.stock = stock;
        this.product = product;
        this.quantity = quantity;
        this.cost = cost;
        this.remaining = quantity;
    }

    public BigDecimal getSubtotal() {
        return cost.multiply(BigDecimal.valueOf(quantity));
    }
}
