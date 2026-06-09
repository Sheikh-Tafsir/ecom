package com.example.demo.product.dto;

import com.example.demo.common.model.Category;
import com.example.demo.common.model.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class ProductListResponse {

    Long id;
    String name;
    BigDecimal price;
    BigDecimal rating;
    int quantity;
    String image;
    Set<String> categories;

    public ProductListResponse(Product product) {
        id = product.getId();
        name = product.getName();
        price = product.getPrice();
        rating = product.getRating();
        quantity = product.getQuantity();
        image = product.getFirstImage();
        categories = product.getCategories().stream()
                .map(Category::getName)
                .collect(Collectors.toSet());
    }
}
