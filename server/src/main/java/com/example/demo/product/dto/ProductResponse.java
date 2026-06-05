package com.example.demo.product.dto;

import com.example.demo.common.model.Category;
import com.example.demo.common.model.Product;
import com.example.demo.common.model.ProductImage;
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

    Long id;
    String name;
    BigDecimal price;
    BigDecimal rating;
    int quantity;
    String image;
    Set<String> categories;

    public ProductResponse(Product product) {
        id = product.getId();
        name = product.getName();
        price = product.getPrice();
        rating = product.getRating();
        quantity = product.getQuantity();
        image = getImage(product);
        categories = product.getCategorise().stream()
                .map(Category::getName)
                .collect(Collectors.toSet());
    }

    private String getImage(Product product) {
        ProductImage image = product.getImages().stream().findFirst().orElse(null);
        return image != null ? image.getImage() : null;
    }
}
