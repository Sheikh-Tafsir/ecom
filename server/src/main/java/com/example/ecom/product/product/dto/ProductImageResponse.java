package com.example.ecom.product.product.dto;

import com.example.ecom.common.model.ProductImage;
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
