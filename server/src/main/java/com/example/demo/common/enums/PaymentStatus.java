package com.example.demo.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentStatus {

    PENDING("Pending"),
    SUCCEEDED("Succeeded"),
    FAILED("Failed");

    @JsonValue
    private final String value;
}
