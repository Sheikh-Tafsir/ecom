package com.example.demo.order.dto;

import com.example.demo.common.model.OrderItem;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemResponse {
    Long id;
    Long orderId;
    Long productId;
    String productName;
    BigDecimal productPrice;
    int quantity;
    BigDecimal subtotal;

    public OrderItemResponse(OrderItem item) {
        id = item.getId();
        orderId = item.getOrder().getId();
        productId = item.getProduct().getId();
        productName = item.getProduct().getName();
        productPrice = item.getProduct().getPrice();
        quantity = item.getQuantity();
        subtotal = item.getSubtotal();
    }
}
