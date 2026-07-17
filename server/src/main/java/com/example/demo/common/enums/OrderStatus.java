package com.example.demo.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatus {

    PENDING("Pending"),
    ACCEPTED("Accepted"),
    CANCELLED("Cancelled"), // canceled by customer
    REJECTED("Rejected"),
    SHIPPED("Shipped"),     // dispatched
    DELIVERED("Delivered"), // received by customer
    COMPLETED("Completed"),   // canceled by admin
    LOST("Lost"), // lost while delivery
    RETURNED("Returned"),
    REFUNDED("Refunded");

    @JsonValue
    private final String value;

    @JsonCreator
    public static OrderStatus fromValue(String value) {
        if (value == null) return null;

        for (OrderStatus status : values()) {
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Invalid Order Status: " + value);
    }

    public boolean isCancellationOrRejection() {
        return this == CANCELLED || this == REJECTED;
    }

    public boolean canTransitionTo(OrderStatus newStatus) {
        if (newStatus == null) {
            return false;
        }

        return switch (this) {
            case PENDING -> newStatus == ACCEPTED
                    || newStatus == CANCELLED
                    || newStatus == REJECTED;

            case ACCEPTED -> newStatus == SHIPPED
                    || newStatus == REJECTED
                    || newStatus == CANCELLED
                    || newStatus == REFUNDED;

            case SHIPPED -> newStatus == DELIVERED
                    || newStatus == LOST
                    || newStatus == RETURNED;

            case DELIVERED -> newStatus == COMPLETED;

            case LOST, RETURNED -> newStatus == REFUNDED;

            case CANCELLED, REJECTED, COMPLETED, REFUNDED -> false;
        };
    }
}

