package com.example.ecom.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.stream.Stream;

public enum BlogPostStatus {
    DRAFT("DRAFT"),
    PUBLISHED("PUBLISHED");

    private final String value;

    BlogPostStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static BlogPostStatus fromValue(String value) {
        return Stream.of(BlogPostStatus.values())
                .filter(status -> status.value.equalsIgnoreCase(value))
                .findFirst()
                .orElse(null);
    }
}
