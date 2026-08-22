package com.example.ecom.product.category.dto;

import com.example.ecom.common.model.Category;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;

    public CategoryResponse(Category category) {
        this.id = category.getId();
        this.name = category.getName();
    }
}
