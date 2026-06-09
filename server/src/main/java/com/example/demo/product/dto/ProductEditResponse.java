package com.example.demo.product.dto;

import com.example.demo.common.model.Category;
import com.example.demo.common.model.Product;
import com.example.demo.common.model.ProductImage;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Set;

@Getter
@AllArgsConstructor
public class ProductEditResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Set<ProductImage> images;
    private Set<Category> categories;

    public ProductEditResponse(Product product) {
        id = product.getId();
        name = product.getName();
        description = product.getDescription();
        price = product.getPrice();
        images = product.getImages();
        categories = product.getCategories();
    }
}
