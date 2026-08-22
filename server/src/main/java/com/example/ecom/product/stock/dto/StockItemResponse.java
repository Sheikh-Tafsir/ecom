package com.example.ecom.product.stock.dto;

import com.example.ecom.common.model.StockItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockItemResponse {

    private Long id;
    private Long stockId;
    private Long productId;
    private String productName;
    private int quantity;
    private BigDecimal purchasePrice;
    private int remaining;
    private BigDecimal subtotal;
    private Instant createdAt;

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
        createdAt = item.getCreatedAt();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStockId() { return stockId; }
    public void setStockId(Long stockId) { this.stockId = stockId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
    public int getRemaining() { return remaining; }
    public void setRemaining(int remaining) { this.remaining = remaining; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
