package com.example.ecom.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Permission {

    SUPER_ADMIN_ACCESS("super_admin:access"),
    ADMIN_ACCESS("admin:access"),
    DELIVERY_MAN_ACCESS("delivery_man:access");

    private final String value;

    Permission(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Permission fromValue(String value) {
        if (value == null) return null;

        for (Permission permission : values()) {
            if (permission.value.equalsIgnoreCase(value) || permission.name().equalsIgnoreCase(value)) {
                return permission;
            }
        }

        throw new IllegalArgumentException("Invalid Permission: " + value);
    }
}
