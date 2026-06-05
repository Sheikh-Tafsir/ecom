package com.example.demo.stock.dto;

import com.example.demo.common.model.Stock;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockListResponse {

    Long id;
    BigDecimal totalCost;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public StockListResponse(Stock stock) {
        id = stock.getId();
        totalCost = stock.getTotalCost();
        createdAt = stock.getCreatedAt();
        updatedAt = stock.getUpdatedAt();
    }
}
