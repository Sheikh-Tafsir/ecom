package com.example.ecom.product.product.adapter;

import com.example.ecom.common.client.product.ProductBasicDto;
import com.example.ecom.common.client.product.ProductClient;
import com.example.ecom.product.product.model.Product;
import com.example.ecom.product.product.model.ProductImage;
import com.example.ecom.product.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.UUID;

/**
 * Internal REST endpoint for cross-service product operations.
 * Secured by {@code X-Internal-Service-Key} at the Spring Security layer.
 * In monolith mode, these endpoints exist but are unused
 * (callers use {@link ProductClientAdapter} directly).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/products")
public class ProductInternalController implements ProductClient {

    private final ProductService productService;

    @GetMapping("/{id}")
    @Override
    public ProductBasicDto findProductById(@PathVariable UUID id) {
        Product product = productService.findByIdHelper(id);
        String firstImage = product.getImages() != null && !product.getImages().isEmpty()
                ? product.getImages().stream()
                        .sorted(Comparator.comparing(ProductImage::getId))
                        .map(ProductImage::getImage)
                        .findFirst().orElse(null)
                : null;
        return new ProductBasicDto(product.getId(), product.getName(), firstImage, product.getPrice(), product.getQuantity());
    }

    @GetMapping("/{id}/has-stock/{quantity}")
    @Override
    public boolean hasStock(@PathVariable UUID id, @PathVariable int quantity) {
        Product product = productService.findByIdHelper(id);
        return product.getQuantity() >= quantity;
    }

    @PostMapping("/{id}/reserve-stock/{quantity}")
    @Override
    public void reserveStock(@PathVariable UUID id, @PathVariable int quantity) {
        productService.decreaseForOrder(id, quantity);
    }

    @PostMapping("/{id}/release-stock/{quantity}")
    @Override
    public void releaseStock(@PathVariable UUID id, @PathVariable int quantity) {
        productService.increaseQuantity(id, quantity);
    }
}
