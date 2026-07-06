package com.example.demo.product.dto;

import com.example.demo.category.dto.CategoryResponse;
import com.example.demo.common.enums.ProductStatus;
import com.example.demo.common.model.Product;
import lombok.*;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private int quantity;
    private ProductStatus status;
    private BigDecimal rating;
    private long reviewCount;
    private Set<ProductImageResponse> images;
    private Set<CategoryResponse> categories;

    public ProductResponse(Product product) {
        id = product.getId();
        name = product.getName();
        description = product.getDescription();
        price = product.getPrice();
        quantity = product.getQuantity();
        status = product.getStatus();
        rating = product.getRating();
        reviewCount = product.getReviewCount();
        images = product.getImages().stream().map(ProductImageResponse::new).collect(Collectors.toSet());
        categories = product.getCategories().stream().map(CategoryResponse::new).collect(Collectors.toSet());
    }
}
