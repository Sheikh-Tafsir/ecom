package com.example.ecom.order.dto;

import com.example.ecom.common.enums.OrderStatus;
import com.example.ecom.common.enums.PaymentMethod;
import com.example.ecom.common.model.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderListResponse {
    private UUID id;
    private UUID userId;
    private String userName;
    private BigDecimal totalPrice;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private boolean paid;
    private Instant createdAt;
    private Instant updatedAt;

    public OrderListResponse(Order order) {
        id = order.getId();
        if (order.getUser() != null) {
            userId = order.getUser().getId();
            userName = order.getUser().getName();
        }
        totalPrice = order.getTotalPrice();
        status = order.getStatus();
        paymentMethod = order.getPaymentMethod();
        paid = order.isPaid();
        createdAt = order.getCreatedAt();
        updatedAt = order.getUpdatedAt();
    }

}
