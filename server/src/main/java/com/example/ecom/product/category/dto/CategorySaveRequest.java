package com.example.ecom.product.category.dto;

import jakarta.validation.constraints.NotBlank;

public record CategorySaveRequest(
        @NotBlank String name
) {
}
