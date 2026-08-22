package com.example.ecom.sale.dto;

import com.example.ecom.common.model.ProductImage;
import com.example.ecom.common.model.Sale;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal profit;
    private int quantity;
    private Instant createdAt;

    public SaleResponse(Sale sale) {
        this.id = sale.getId();
        if (sale.getProduct() != null) {
            this.productId = sale.getProduct().getId();
            this.productName = sale.getProduct().getName();
            if (sale.getProduct().getImages() != null) {
                this.productImage = sale.getProduct().getImages().stream()
                        .findFirst().map(ProductImage::getImage).orElse(null);
            }
        }
        this.profit = sale.getProfit();
        this.quantity = sale.getQuantity();
        this.createdAt = sale.getCreatedAt();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public BigDecimal getProfit() { return profit; }
    public void setProfit(BigDecimal profit) { this.profit = profit; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
