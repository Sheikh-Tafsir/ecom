package com.example.demo.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Gender {

    MALE("Male"),
    FEMALE("Female");

    @JsonValue
    private final String value;

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
