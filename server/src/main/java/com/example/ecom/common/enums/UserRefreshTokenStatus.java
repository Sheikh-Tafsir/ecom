package com.example.ecom.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UserRefreshTokenStatus {

    ACTIVE("Active"),
    REVOKED("Revoked");

    private final String value;

    UserRefreshTokenStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static UserRefreshTokenStatus fromValue(String value) {
        if (value == null) return null;

        for (UserRefreshTokenStatus status : values()) {
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Invalid User Refresh Token Status: " + value);
    }
}
