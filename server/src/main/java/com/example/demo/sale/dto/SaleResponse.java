package com.example.demo.sale.dto;

import com.example.demo.common.model.ProductImage;
import com.example.demo.common.model.Sale;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SaleResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal profit;
    private int quantity;
    private LocalDateTime createdAt;

    public SaleResponse(Sale sale) {
        this.id = sale.getId();
        this.productId = sale.getProduct().getId();
        this.productName = sale.getProduct().getName();
        this.productImage = sale.getProduct().getImages().stream()
                .findFirst().map(ProductImage::getImage).orElse(null);
        this.profit = sale.getProfit();
        this.quantity = sale.getQuantity();
        this.createdAt = sale.getCreatedAt();
    }
}
