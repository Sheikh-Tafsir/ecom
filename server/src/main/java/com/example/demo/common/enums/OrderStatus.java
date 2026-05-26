package com.example.demo.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatus {

    CREATED("Created"),
    CONFIRMED("Confirmed"),
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
}

