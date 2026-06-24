package com.example.demo.order.dto;

import com.example.demo.common.model.OrderItem;
import com.example.demo.common.model.ProductImage;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class OrderItemResponse {
    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal productPrice;
    private int quantity;
    private BigDecimal subtotal;

    public OrderItemResponse(OrderItem item) {
        id = item.getId();
        orderId = item.getOrder().getId();
        productId = item.getProduct().getId();
        productName = item.getProduct().getName();
        productImage = item.getProduct().getImages().stream()
                .findFirst().map(ProductImage::getImage).orElse(null);
        productPrice = item.getProduct().getPrice();
        quantity = item.getQuantity();
        subtotal = item.getSubtotal();
    }
}
