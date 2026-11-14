package com.example.ecom.product.product.adapter;

import com.example.ecom.common.client.product.ProductBasicDto;
import com.example.ecom.common.client.product.ProductClient;
import com.example.ecom.product.product.model.Product;
import com.example.ecom.product.product.model.ProductImage;
import com.example.ecom.product.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.UUID;

/**
 * Monolith adapter: implements {@link ProductClient} by delegating to {@link ProductService}.
 * On microservice extraction, this class is removed and replaced by an HTTP proxy.
 */
@Primary
@Service
@RequiredArgsConstructor
public class ProductClientAdapter implements ProductClient {

    private final ProductService productService;

    @Override
    public ProductBasicDto findProductById(UUID id) {
        Product product = productService.findByIdHelper(id);
        String firstImage = product.getImages() != null && !product.getImages().isEmpty()
                ? product.getImages().stream()
                        .sorted(Comparator.comparing(ProductImage::getId))
                        .map(ProductImage::getImage)
                        .findFirst().orElse(null)
                : null;
        return new ProductBasicDto(product.getId(), product.getName(), firstImage, product.getPrice(), product.getQuantity());
    }

    /**
     * Checks stock availability for a single product.
     * Note: Future microservice scaling may introduce a batch endpoint hasStockAll(Map<UUID, Integer>)
     * to eliminate N+1 calls across network.
     */
    @Override
    public boolean hasStock(UUID id, int quantity) {
        Product product = productService.findByIdHelper(id);
        return product.getQuantity() >= quantity;
    }

    @Override
    public void reserveStock(UUID id, int quantity) {
        productService.decreaseForOrder(id, quantity);
    }

    @Override
    public void releaseStock(UUID id, int quantity) {
        productService.increaseQuantity(id, quantity);
    }
}
