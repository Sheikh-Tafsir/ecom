package com.example.demo.common.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Stock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "stock", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<StockItem> items = new HashSet<>();

    @Column(nullable = false)
    private BigDecimal totalCost = BigDecimal.ZERO;

    public void addItem(Product product, int quantity, BigDecimal cost) {
        StockItem item = new StockItem(this, product, quantity, cost);
        items.add(item);

        calculateTotal();
    }

    public void removeItem(StockItem item) {
        item.setStock(null);
        items.remove(item);

        calculateTotal();
    }

    public void calculateTotal() {
        this.totalCost = items.stream()
                .map(StockItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
