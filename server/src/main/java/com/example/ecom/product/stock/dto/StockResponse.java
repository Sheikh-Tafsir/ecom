package com.example.ecom.product.stock.dto;

import com.example.ecom.common.model.Stock;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockResponse {

    private UUID id;
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

}
