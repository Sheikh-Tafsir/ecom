package com.example.demo.stock.dto;

import com.example.demo.common.model.StockItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockItemResponse {

    Long id;
    Long stockId;
    Long productId;
    String productName;
    int quantity;
    BigDecimal purchasePrice;
    int remaining;
    BigDecimal subtotal;
    LocalDateTime createdAt;

    public StockItemResponse(StockItem item) {
        id = item.getId();
        stockId = item.getStock().getId();
        productId = item.getProduct().getId();
        productName = item.getProduct().getName();
        quantity = item.getQuantity();
        purchasePrice = item.getPurchasePrice();
        remaining = item.getRemaining();
        subtotal = item.getSubtotal();
        createdAt = item.getCreatedAt();
    }
}
