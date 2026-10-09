package com.example.ecom.product.stock.dto;

import com.example.ecom.common.model.Stock;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockListResponse {

    private UUID id;
    private BigDecimal totalCost;
    private Instant createdAt;
    private Instant updatedAt;

    public StockListResponse(Stock stock) {
        id = stock.getId();
        totalCost = stock.getTotalCost();
        createdAt = stock.getCreatedAt();
        updatedAt = stock.getUpdatedAt();
    }

}
