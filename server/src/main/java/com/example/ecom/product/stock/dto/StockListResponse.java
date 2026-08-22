package com.example.ecom.product.stock.dto;

import com.example.ecom.common.model.Stock;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockListResponse {

    private Long id;
    private BigDecimal totalCost;
    private Instant createdAt;
    private Instant updatedAt;

    public StockListResponse(Stock stock) {
        id = stock.getId();
        totalCost = stock.getTotalCost();
        createdAt = stock.getCreatedAt();
        updatedAt = stock.getUpdatedAt();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
