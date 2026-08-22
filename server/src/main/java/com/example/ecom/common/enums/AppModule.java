package com.example.ecom.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AppModule {

    USER("User"),
    ORDER("Order"),
    SALE("Sale");

    private final String value;

    AppModule(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static AppModule fromValue(String value) {
        if (value == null) return null;

        for (AppModule modules : values()) {
            if (modules.value.equalsIgnoreCase(value) || modules.name().equalsIgnoreCase(value)) {
                return modules;
            }
        }

        throw new IllegalArgumentException("Invalid module: " + value);
    }
}
