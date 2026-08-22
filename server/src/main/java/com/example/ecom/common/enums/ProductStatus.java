package com.example.ecom.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ProductStatus {

    COMING_SOON("Coming Soon"),

    AVAILABLE("Available"),

    STOCK_OUT("Stock Out"),

    DISCONTINUED("Discontinued");

    private final String value;

    ProductStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

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
