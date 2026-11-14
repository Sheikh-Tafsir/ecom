package com.example.ecom.order.dto;

import com.example.ecom.order.model.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private UUID id;
    private UUID orderId;
    private UUID productId;
    private String productName;
    private String productImage;
    private BigDecimal productPrice;
    private int quantity;
    private BigDecimal subtotal;

    public OrderItemResponse(OrderItem item) {
        id = item.getId();
        orderId = item.getOrder() != null ? item.getOrder().getId() : null;
        productId = item.getProductId();
        productName = item.getProductName();
        productImage = item.getProductImage();
        productPrice = item.getUnitPrice();
        quantity = item.getQuantity();
        subtotal = item.getSubtotal();
    }
}
