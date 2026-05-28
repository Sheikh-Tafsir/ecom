package com.example.demo.stock.dto;

import com.example.demo.common.model.StockItem;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockItemResponse {

    Long id;
    Long stockId;
    Long productId;
    String productName;
    BigDecimal productPrice;
    int quantity;
    BigDecimal cost;
    int remaining;
    BigDecimal subtotal;

    public StockItemResponse(StockItem item) {
        id = item.getId();
        stockId = item.getStock().getId();
        productId = item.getProduct().getId();
        productName = item.getProduct().getName();
        productPrice = item.getProduct().getPrice();
        quantity = item.getQuantity();
        cost = item.getCost();
        remaining = item.getRemaining();
        subtotal = item.getSubtotal();
    }
}
