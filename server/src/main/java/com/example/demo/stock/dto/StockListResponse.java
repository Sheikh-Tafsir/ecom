package com.example.demo.stock.dto;

import com.example.demo.common.model.Stock;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
