package com.example.demo.product.dto;

import com.example.demo.common.model.ProductImage;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageResponse {
    private Long id;
    private String image;

    public ProductImageResponse(ProductImage productImage) {
        this.id = productImage.getId();
        this.image = productImage.getImage();
    }
}
