package com.example.ecom.product.category.dto;

import com.example.ecom.common.model.Category;

import java.util.UUID;

public record CategoryResponse(UUID id, String name) {

    public CategoryResponse(Category category) {
        this(category.getId(), category.getName());
    }
}
