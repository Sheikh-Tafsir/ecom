package com.example.demo.product.dto;

import com.example.demo.common.enums.ProductStatus;
import com.example.demo.common.model.Category;
import com.example.demo.common.model.Product;
import com.example.demo.common.model.ProductImage;
import com.example.demo.review.dto.ReviewResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Set<String> images;
    private Set<String> categories;
    private Set<ReviewResponse> reviews;

    public ProductResponse(Product product) {
        id = product.getId();
        name = product.getName();
        description = product.getDescription();
        price = product.getPrice();
        quantity = product.getQuantity();
        status = product.getStatus();
        rating = product.getRating();
        images = product.getImages().stream()
                .map(ProductImage::getImage)
                .collect(Collectors.toSet());
        categories = product.getCategories().stream()
                .map(Category::getName)
                .collect(Collectors.toSet());
        reviews = product.getReviews().stream()
                .map(ReviewResponse::new)
                .collect(Collectors.toSet());
    }
}
