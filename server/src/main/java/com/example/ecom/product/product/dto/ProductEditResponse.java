package com.example.ecom.product.product.dto;

import com.example.ecom.product.category.dto.CategoryResponse;
import com.example.ecom.common.model.Product;
import lombok.*;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductEditResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Set<ProductImageResponse> images;
    private Set<CategoryResponse> categories;

    public ProductEditResponse(Product product) {
        id = product.getId();
        name = product.getName();
        description = product.getDescription();
        price = product.getPrice();
        images = product.getImages().stream().map(ProductImageResponse::new).collect(Collectors.toSet());
        categories = product.getCategories().stream().map(CategoryResponse::new).collect(Collectors.toSet());
    }
}
