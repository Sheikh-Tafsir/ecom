package com.example.demo.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationType {

    INFO("Info"),
    SUCCESS("Success"),
    ERROR("Error"),
    WARNING("Warning");

    @JsonValue
    private final String value;

    @JsonCreator
    public static NotificationType fromValue(String value) {
        if (value == null) return null;

        for (NotificationType status : values()) {
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Invalid Notification Status: " + value);
    }
}
