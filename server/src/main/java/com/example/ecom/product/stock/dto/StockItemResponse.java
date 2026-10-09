package com.example.ecom.product.stock.dto;

import com.example.ecom.common.model.StockItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockItemResponse {

    private UUID id;
    private UUID stockId;
    private UUID productId;
    private String productName;
    private int quantity;
    private BigDecimal purchasePrice;
    private int remaining;
    private BigDecimal subtotal;

    public StockItemResponse(StockItem item) {
        id = item.getId();
        if (item.getStock() != null) {
            stockId = item.getStock().getId();
        }
        if (item.getProduct() != null) {
            productId = item.getProduct().getId();
            productName = item.getProduct().getName();
        }
        quantity = item.getQuantity();
        purchasePrice = item.getPurchasePrice();
        remaining = item.getRemaining();
        subtotal = item.getSubtotal();
    }

}
