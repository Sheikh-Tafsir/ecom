package com.example.demo.product.dto;

import com.example.demo.common.enums.ProductStatus;
import com.example.demo.common.model.Category;
import com.example.demo.common.model.Product;
import com.example.demo.common.model.ProductImage;
import com.example.demo.review.dto.ReviewResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
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
    private Set<ProductImage> images;
    private Set<Category> categories;
    private Set<ReviewResponse> reviews;

    public ProductResponse(Product product) {
        id = product.getId();
        name = product.getName();
        description = product.getDescription();
        price = product.getPrice();
        quantity = product.getQuantity();
        status = product.getStatus();
        rating = product.getRating();
        reviewCount = product.getReviewCount();
        images = product.getImages();
        categories = product.getCategories();
        reviews = product.getReviews().stream()
                .map(ReviewResponse::new)
                .collect(Collectors.toSet());
    }
}
