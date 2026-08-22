package com.example.ecom.order.dto;

import com.example.ecom.common.enums.OrderStatus;
import com.example.ecom.common.enums.PaymentMethod;
import com.example.ecom.common.model.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
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
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Set<OrderItemResponse> getItems() { return items; }
    public void setItems(Set<OrderItemResponse> items) { this.items = items; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
