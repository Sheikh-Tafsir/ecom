package com.example.ecom.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RoleName {

    SUPER_ADMIN("Super Admin"),
    ADMIN("Admin"),
    DELIVERY_MAN("Delivery Main"),
    USER("User");

    private final String value;

    RoleName(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static RoleName fromValue(String value) {
        if (value == null) return null;

        for (RoleName roleName : values()) {
            if (roleName.value.equalsIgnoreCase(value) || roleName.name().equalsIgnoreCase(value)) {
                return roleName;
            }
        }

        throw new IllegalArgumentException("Invalid Role: " + value);
    }
}
