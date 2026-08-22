package com.example.ecom.product.stock.dto;

import com.example.ecom.common.model.Stock;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockResponse {

    private Long id;
    private BigDecimal totalCost;
    private Set<StockItemResponse> items;
    private Instant createdAt;
    private Instant updatedAt;

    public StockResponse(Stock stock) {
        id = stock.getId();
        totalCost = stock.getTotalCost();
        items = stock.getItems()
                .stream()
                .map(StockItemResponse::new).collect(Collectors.toSet());
        createdAt = stock.getCreatedAt();
        updatedAt = stock.getUpdatedAt();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public Set<StockItemResponse> getItems() { return items; }
    public void setItems(Set<StockItemResponse> items) { this.items = items; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
