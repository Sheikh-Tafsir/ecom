package com.example.demo.category.dto;

import jakarta.validation.constraints.NotBlank;

public record CategorySaveRequest(
        @NotBlank String name
) {
}
