package com.example.demo.order.dto;

import com.example.demo.common.enums.OrderStatus;
import com.example.demo.common.model.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OrderListResponse {
    Long id;
    Long userId;
    String userName;
    BigDecimal totalPrice;
    OrderStatus status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public OrderListResponse(Order order) {
        id = order.getId();
        userId = order.getUser().getId();
        userName = order.getUser().getName();
        totalPrice = order.getTotalPrice();
        status = order.getStatus();
        createdAt = order.getCreatedAt();
        updatedAt = order.getUpdatedAt();
    }
}
