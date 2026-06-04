package com.example.demo.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ShipmentStatus {

    CONFIRMED("Confirmed"),
    SHIPPED("Shipped"),
    OUT_FOR_DELIVERY("Out for delivery"),
    DELIVERED("Deliver");

    @JsonValue
    private final String value;
}
