package com.example.demo.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProductStatus {

    COMING_SOON("Coming Soon"),

    AVAILABLE("Available"),

    STOCK_OUT("Stock Out"),

    DISCONTINUED("Discontinued");

    @JsonValue
    private final String value;

    @JsonCreator
    public static ProductStatus fromValue(String value) {
        if (value == null) return null;

        for (ProductStatus status : values()) {
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Invalid Product Status: " + value);
    }
}
