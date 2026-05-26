package com.example.demo.product.helper;

import com.example.demo.common.model.Product;
import com.example.demo.common.model.ProductImage;
import com.example.demo.product.dto.ProductRequest;
import com.example.demo.product.dto.ProductResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductHelper {

    public Product toEntity(ProductRequest request) {
        Product product = new Product();
        updateEntity(product, request);
        return product;
    }

    public void updateEntity(Product product, ProductRequest request) {
        product.setName(request.name());
        product.setPrice(request.price());
        product.setQuantity(request.quantity());
    }

    public void addImage(Product product, String imageUrl) {
        ProductImage image = new ProductImage();
        image.setImage(imageUrl);
        product.addImage(image);
    }

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getQuantity(),
                product.getImages().stream().map(ProductImage::getImage).toList(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
