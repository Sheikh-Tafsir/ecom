package com.example.ecom.product.stock.model;

import com.example.ecom.common.model.AuditableEntity;
import com.example.ecom.product.product.model.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "stocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Stock extends AuditableEntity {

    @OneToMany(mappedBy = "stock", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<StockItem> items = new HashSet<>();

    @Column(nullable = false)
    private BigDecimal totalCost = BigDecimal.ZERO;

    public void addItem(Product product, int quantity, BigDecimal purchasePrice) {
        StockItem item = new StockItem(this, product, quantity, purchasePrice);
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
