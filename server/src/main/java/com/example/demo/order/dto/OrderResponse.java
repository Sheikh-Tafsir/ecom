package com.example.demo.order.dto;

import com.example.demo.common.enums.OrderStatus;
import com.example.demo.common.enums.PaymentMethod;
import com.example.demo.common.model.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class OrderResponse {
    Long id;
    Long userId;
    String userName;
    BigDecimal totalPrice;
    OrderStatus status;
    String name;
    String address;
    String phone;
    Set<OrderItemResponse> items;
    PaymentMethod paymentMethod;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public OrderResponse(Order order) {
        id = order.getId();
        userId = order.getUser().getId();
        userName = order.getUser().getName();
        totalPrice = order.getTotalPrice();
        status = order.getStatus();
        name = order.getName();
        address = order.getAddress();
        phone = order.getPhone();
        items = order.getItems()
                .stream()
                .map(OrderItemResponse::new).collect(Collectors.toSet());
        paymentMethod = order.getPaymentMethod();
        createdAt = order.getCreatedAt();
        updatedAt = order.getUpdatedAt();
    }
}
