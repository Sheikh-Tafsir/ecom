package com.example.demo.product.service;

import com.example.demo.common.model.Product;
import com.example.demo.common.model.ProductImage;
import com.example.demo.common.service.CloudinaryService;
import com.example.demo.common.service.MessageService;
import com.example.demo.product.dto.CreateProductRequest;
import com.example.demo.product.dto.UpdateProductRequest;
import com.example.demo.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

import static com.example.demo.common.enums.ProductStatus.DISCONTINUED;
import static com.example.demo.common.utils.FileUtils.fileExists;
import static com.example.demo.common.utils.SecurityUtil.isAdmin;
import static com.example.demo.common.utils.Utils.isEmpty;
import static com.example.demo.common.utils.Utils.getValidPageable;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ModelMapper modelMapper;

    private final ProductRepository productRepository;

    private final CloudinaryService cloudinaryService;

    private final MessageService messageService;

    public Page<Product> findAll(Pageable pageable, String name) {
        return productRepository.findAllByNameAndExcludeStatus(name, isAdmin() ? null : DISCONTINUED, getValidPageable(pageable));
    }

    public Product findById(Long id) {
        Product product = findByIdHelper(id);
        if (!isAdmin()) {
            throw new AccessDeniedException("Product discontinued, cannot access by regular user");
        }

        return product;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Product create(CreateProductRequest request) throws IOException {
        Product product = modelMapper.map(request, Product.class);
        addImages(product, request.images());

        return productRepository.save(product);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Product update(Long id, UpdateProductRequest request) throws IOException {
        Product product = findByIdHelper(id);
        modelMapper.map(request, product);

        product.getImages().removeIf(
                image -> !request.keptImageIds().contains(image.getId())
        );

        if (product.getImages().size() + request.images().size() > 5) {
            throw new RuntimeException("Cannot upload more than 5 files");
        }

        addImages(product, request.images());

        return productRepository.save(product);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(Long id) {
        Product product = findByIdHelper(id);
        product.setStatus(DISCONTINUED);
        productRepository.save(product);
    }

    public Product findByIdHelper(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "Product", id)));
    }

    private void addImages(Product product, Set<MultipartFile> images) throws IOException {
        if (isEmpty(images)) {
            return;
        }

        for (MultipartFile imageFile : images) {
            if (fileExists(imageFile)) {
                String imageUrl = cloudinaryService.uploadFile(imageFile);
                ProductImage image = new ProductImage();
                image.setImage(imageUrl);
                product.addImage(image);
            }
        }
    }

//    public void increaseQuantity(Product product, int quantity) {
//        product.setQuantity(product.getQuantity() + quantity);
//    }
//
//    public void decreaseQuantity(Product product, int quantity) {
//        if (product.getQuantity() < quantity) {
//            throw new RuntimeException("Product stock is not available for product id: " + product.getId());
//        }
//
//        product.setQuantity(product.getQuantity() - quantity);
//    }
}
