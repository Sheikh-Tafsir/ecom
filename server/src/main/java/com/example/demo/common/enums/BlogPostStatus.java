package com.example.demo.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum BlogPostStatus {
    DRAFT("DRAFT"),
    PUBLISHED("PUBLISHED");

    @JsonValue
    private final String value;

    @JsonCreator
    public static BlogPostStatus fromValue(String value) {
        return Stream.of(BlogPostStatus.values())
                .filter(status -> status.value.equalsIgnoreCase(value))
                .findFirst()
                .orElse(null);
    }
}
