package com.example.demo.stock.dto;

import com.example.demo.common.model.Stock;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class StockResponse {

    Long id;
    BigDecimal totalCost;
    Set<StockItemResponse> items;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public StockResponse(Stock stock) {
        id = stock.getId();
        totalCost = stock.getTotalCost();
        items = stock.getItems()
                .stream()
                .map(StockItemResponse::new).collect(Collectors.toSet());
        createdAt = stock.getCreatedAt();
        updatedAt = stock.getUpdatedAt();
    }
}
