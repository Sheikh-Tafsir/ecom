package com.example.ecom.order.dto;

import com.example.ecom.common.enums.OrderStatus;
import com.example.ecom.common.enums.PaymentMethod;
import com.example.ecom.common.model.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderListResponse {
    private Long id;
    private Long userId;
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
