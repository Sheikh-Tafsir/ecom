package com.example.demo.product.service;

import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.enums.ProductStatus;
import com.example.demo.common.model.Product;
import com.example.demo.common.model.ProductImage;
import com.example.demo.common.service.CloudinaryService;
import com.example.demo.common.service.MessageService;
import com.example.demo.product.dto.CreateProductRequest;
import com.example.demo.product.dto.ProductResponse;
import com.example.demo.product.dto.UpdateProductRequest;
import com.example.demo.product.repository.CategoryRepository;
import com.example.demo.product.repository.ProductRepository;
import com.example.demo.stock.dto.CreateStockItemRequest;
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
import java.util.HashSet;
import java.util.Set;

import static com.example.demo.common.enums.ProductStatus.DISCONTINUED;
import static com.example.demo.common.utils.FileUtils.fileExists;
import static com.example.demo.common.utils.SecurityConstants.HAS_ROLE_ADMIN;
import static com.example.demo.common.utils.SecurityUtil.isAdmin;
import static com.example.demo.common.utils.Utils.*;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ModelMapper modelMapper;

    private final ProductRepository productRepository;

    private final CategoryRepository categoryRepository;

    private final CloudinaryService cloudinaryService;

    private final MessageService messageService;

    @Transactional(readOnly = true)
    public Page<ProductResponse> findAll(Pageable pageable, String name, String category, CustomUserDetails userDetails) {
        String nameFilter = (isNull(name)) ? null : "%" + name + "%";
        return productRepository.findAllByNameAndExcludeStatus(nameFilter, category, isAdmin(userDetails) ? null : DISCONTINUED, getValidPageable(pageable))
                .map(ProductResponse::new);
    }

    public Product findById(Long id, CustomUserDetails userDetails) {
        Product product = findByIdHelper(id);
        if (product.getStatus() == DISCONTINUED && !isAdmin(userDetails)) {
            throw new AccessDeniedException("User with id: " + userDetails.getId()  + " attempted to access discontinued Product with id: " + id);
        }

        return product;
    }

    @PreAuthorize(HAS_ROLE_ADMIN)
    @Transactional
    public Product create(CreateProductRequest request) throws IOException {
        Product product = new Product();
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setCategorise(new HashSet<>(categoryRepository.findAllById(request.getCategoryIds())));

        addImages(product, request.getImages());

        return productRepository.save(product);
    }

    @PreAuthorize(HAS_ROLE_ADMIN)
    @Transactional
    public Product update(Long id, UpdateProductRequest request) throws IOException {
        Product product = findByIdHelper(id);
        
        product.setName(request.name());
        product.setPrice(request.price());
        product.setCategorise(new HashSet<>(categoryRepository.findAllById(request.categoryIds())));

        product.getImages().removeIf(
                image -> !request.keptImageIds().contains(image.getId())
        );

        if (product.getImages().size() + (request.images() != null ? request.images().size() : 0) > 5) {
            throw new RuntimeException("Cannot upload more than 5 files");
        }

        addImages(product, request.images());

        return productRepository.save(product);
    }

    @PreAuthorize(HAS_ROLE_ADMIN)
    @Transactional
    public void delete(Long id) {
        Product product = findByIdHelper(id);
        product.setStatus(DISCONTINUED);
        product.setDeleted(true);
        productRepository.save(product);
    }

    public Product findByIdHelper(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "Product", id)));
    }

    public void consumeForOrder(Product product, int quantity) {
        checkActive(product);

        if (product.getQuantity() < quantity) {
            throw new RuntimeException("Product quantity is not available for product id: " + product.getId());
        }

        product.setQuantity(product.getQuantity() - quantity);
        productRepository.save(product);
    }

    public Product updateForStock(CustomUserDetails userDetails, CreateStockItemRequest itemRequest) {
        Product product = findById(itemRequest.productId(), userDetails);
        checkActive(product);

        product.setQuantity(product.getQuantity() + itemRequest.quantity());

        if (product.getStatus() == ProductStatus.COMING_SOON) {
            product.setStatus(ProductStatus.AVAILABLE);
        }

        return product;
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

    private void checkActive(Product product) {
        if (product.isNotActive()) {
            throw new RuntimeException("Product is deleted");
        }
    }
}
