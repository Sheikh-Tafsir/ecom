package com.example.ecom.order.dto;

import com.example.ecom.common.enums.OrderStatus;
import com.example.ecom.common.model.OrderStatusHistory;

import java.time.Instant;
import java.util.UUID;

public record OrderStatusHistoryResponse(
        UUID id,
        OrderStatus fromStatus,
        OrderStatus toStatus,
        UUID changedByUserId,
        String changedByUserName,
        String comment,
        Instant createdAt
) {
    public OrderStatusHistoryResponse(OrderStatusHistory history) {
        this(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getChangedBy() != null ? history.getChangedBy().getId() : null,
                history.getChangedBy() != null ? history.getChangedBy().getName() : "System",
                history.getComment(),
                history.getCreatedAt()
        );
    }
}
