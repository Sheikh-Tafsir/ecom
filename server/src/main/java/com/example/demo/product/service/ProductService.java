package com.example.demo.product.service;

import com.example.demo.common.model.Product;
import com.example.demo.common.service.CloudinaryService;
import com.example.demo.common.service.MessageService;
import com.example.demo.product.dto.ProductRequest;
import com.example.demo.product.dto.ProductResponse;
import com.example.demo.product.helper.ProductHelper;
import com.example.demo.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static com.example.demo.common.utils.FileUtils.fileExists;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    private final ProductHelper productHelper;

    private final CloudinaryService cloudinaryService;

    private final MessageService messageService;

    public Page<ProductResponse> findAll(Pageable pageable, String name) {
        return productRepository.findAllByName(name, pageable).map(productHelper::toResponse);
    }

    public ProductResponse findById(Long id) {
        return productHelper.toResponse(findByIdHelper(id));
    }

    public Product findEntityById(Long id) {
        return findByIdHelper(id);
    }

    @Transactional
    public ProductResponse create(ProductRequest request) throws IOException {
        Product product = productHelper.toEntity(request);
        addImages(product, request);

        return productHelper.toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) throws IOException {
        Product product = findByIdHelper(id);
        productHelper.updateEntity(product, request);

        if (request.images() != null && !request.images().isEmpty()) {
            product.clearImages();
            addImages(product, request);
        }

        return productHelper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        productRepository.delete(findByIdHelper(id));
    }

    public void decreaseStock(Product product, int quantity) {
        if (product.getQuantity() < quantity) {
            throw new ValidationException("Product stock is not available for product id: " + product.getId());
        }

        product.setQuantity(product.getQuantity() - quantity);
    }

    public void increaseStock(Product product, int quantity) {
        product.setQuantity(product.getQuantity() + quantity);
    }

    private Product findByIdHelper(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "Product", id)));
    }

    private void addImages(Product product, ProductRequest request) throws IOException {
        if (request.images() == null) {
            return;
        }

        for (MultipartFile image : request.images()) {
            if (fileExists(image)) {
                productHelper.addImage(product, cloudinaryService.uploadFile(image));
            }
        }
    }
}
