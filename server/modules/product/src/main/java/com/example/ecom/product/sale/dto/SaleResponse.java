package com.example.ecom.product.sale.dto;

import com.example.ecom.product.product.model.ProductImage;
import com.example.ecom.product.sale.model.Sale;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleResponse {
    private UUID id;
    private UUID productId;
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

}
