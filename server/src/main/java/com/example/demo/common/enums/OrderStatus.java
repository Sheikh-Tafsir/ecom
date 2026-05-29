package com.example.demo.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.EnumSet;

@Getter
@AllArgsConstructor
public enum OrderStatus {

    CREATED("Created"),
    ACCEPTED("Accepted"),
    SHIPPED("Shipped"),     // dispatched
    DELIVERED("Delivered"), // received by customer
    PAID("Paid"),           // payment successful
    CANCELLED("Cancelled"), // canceled by customer
    REJECTED("Rejected");   // canceled by admin

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
            case CREATED -> newStatus == ACCEPTED
                    || newStatus == CANCELLED
                    || newStatus == REJECTED;

            case ACCEPTED -> newStatus == SHIPPED
                    || newStatus == CANCELLED;

            case SHIPPED -> newStatus == DELIVERED;

            case DELIVERED -> newStatus == PAID;

            case PAID -> false;

            case CANCELLED, REJECTED -> false;
        };
    }

    public boolean canBeSetByAdmin() {
        return EnumSet.of(ACCEPTED, SHIPPED, DELIVERED, PAID).contains(this);
    }

    public boolean canBeSetByUser() {
        return EnumSet.of(CANCELLED).contains(this);
    }
}

