package com.example.ecom.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Gender {

    MALE("Male"),
    FEMALE("Female");

    private final String value;

    Gender(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Gender fromValue(String value) {
        if (value == null) return null;

        for (Gender gender : values()) {
            if (gender.value.equalsIgnoreCase(value) || gender.name().equalsIgnoreCase(value)) {
                return gender;
            }
        }

        throw new IllegalArgumentException("Invalid Gender: " + value);
    }
}
