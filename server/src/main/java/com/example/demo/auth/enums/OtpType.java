package com.example.demo.auth.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OtpType {

    SIGNUP("Signup"),
    RESET("Reset");

    @JsonValue
    private final String value;

    @JsonCreator
    public static OtpType fromValue(String value) {
        if (value == null) return null;

        for (OtpType otpType : values()) {
            if (otpType.value.equalsIgnoreCase(value) || otpType.name().equalsIgnoreCase(value)) {
                return otpType;
            }
        }

        throw new IllegalArgumentException("Invalid Otp Type: " + value);
    }
}
