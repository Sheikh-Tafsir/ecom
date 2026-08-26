package com.example.ecom.order.dto;

import com.example.ecom.common.enums.OrderStatus;
import com.example.ecom.common.enums.PaymentMethod;
import com.example.ecom.common.model.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private Long userId;
    private String userName;
    private BigDecimal totalPrice;
    private OrderStatus status;
    private String name;
    private String address;
    private String phone;
    private Set<OrderItemResponse> items;
    private List<OrderStatusHistoryResponse> statusHistories;
    private PaymentMethod paymentMethod;
    private boolean paid;
    private Instant createdAt;
    private Instant updatedAt;

    public OrderResponse(Order order) {
        id = order.getId();
        if (order.getUser() != null) {
            userId = order.getUser().getId();
            userName = order.getUser().getName();
        }
        totalPrice = order.getTotalPrice();
        status = order.getStatus();
        name = order.getName();
        address = order.getAddress();
        phone = order.getPhone();
        items = order.getItems()
                .stream()
                .map(OrderItemResponse::new).collect(Collectors.toSet());
        if (order.getStatusHistories() != null) {
            statusHistories = order.getStatusHistories()
                    .stream()
                    .map(OrderStatusHistoryResponse::new)
                    .collect(Collectors.toList());
        }
        paymentMethod = order.getPaymentMethod();
        paid = order.isPaid();
        createdAt = order.getCreatedAt();
        updatedAt = order.getUpdatedAt();
    }
}
