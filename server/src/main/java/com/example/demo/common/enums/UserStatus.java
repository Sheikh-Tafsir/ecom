package com.example.demo.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatus {

    NOT_VERIFIED("Not Verified"),

    ACTIVE("Active"),

    SUSPENDED("Suspended"),

    INACTIVE("Inactive"),

    DELETED("Deleted");

    @JsonValue
    private final String value;

    @JsonCreator
    public static UserStatus fromValue(String value) {
        if (value == null) return null;

        for (UserStatus status : values()) {
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Invalid User Status: " + value);
    }
}
