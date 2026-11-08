package com.example.ecom.order.dto;

import com.example.ecom.order.enums.OrderStatus;
import com.example.ecom.order.model.OrderStatusHistory;

import java.time.Instant;
import java.util.UUID;

public record OrderStatusHistoryResponse(
        UUID id,
        OrderStatus fromStatus,
        OrderStatus toStatus,
        UUID changedById,
        String comment,
        Instant createdAt
) {
    public OrderStatusHistoryResponse(OrderStatusHistory history) {
        this(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getChangedById(),
                history.getComment(),
                history.getCreatedAt()
        );
    }
}
