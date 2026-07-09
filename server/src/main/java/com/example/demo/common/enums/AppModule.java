package com.example.demo.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AppModule {

    USER("User"),
    ORDER("Order"),
    SALE("Sale");

    @JsonValue
    private final String value;

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
