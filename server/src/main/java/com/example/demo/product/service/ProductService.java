package com.example.demo.product.service;

import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.dto.DateRangeDto;
import com.example.demo.common.enums.ProductStatus;
import com.example.demo.common.model.Product;
import com.example.demo.common.model.ProductImage;
import com.example.demo.common.service.fileStorage.FileStorageService;
import com.example.demo.common.service.MessageService;
import com.example.demo.product.dto.*;
import com.example.demo.category.repository.CategoryRepository;
import com.example.demo.product.repository.ProductRepository;
import com.example.demo.stock.dto.CreateStockItemRequest;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.example.demo.common.enums.Permission.ADMIN_ACCESS;
import static com.example.demo.common.enums.Permission.SUPER_ADMIN_ACCESS;
import static com.example.demo.common.enums.ProductStatus.DISCONTINUED;
import static com.example.demo.common.utils.DateUtils.resolveDates;
import static com.example.demo.common.utils.FileUtils.fileExists;
import static com.example.demo.common.utils.SecurityUtil.hasPermission;
import static com.example.demo.common.utils.Utils.*;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    private final CategoryRepository categoryRepository;

    private final FileStorageService fileStorageService;

    private final MessageService messageService;

    public Page<ProductListResponse> findAll(Pageable pageable, String name, String category, LocalDate fromDate,
                                             LocalDate toDate, CustomUserDetails userDetails) {

        DateRangeDto dateRange = resolveDates(fromDate, toDate);

        return productRepository.findAll(getNameFilter(name), category,
                        hasPermission(List.of(SUPER_ADMIN_ACCESS.getValue(), ADMIN_ACCESS.getValue()), userDetails)
                        ? null : DISCONTINUED, dateRange.fromDate(), dateRange.toDate(), getValidPageable(pageable))
                .map(ProductListResponse::new);
    }

    public List<ProductListResponse> search(String name) {
        return productRepository.searchByName(getNameFilter(name), DISCONTINUED, PageRequest.of(0, MAX_SEARCH_SIZE))
                .stream()
                .map(ProductListResponse::new)
                .toList();
    }

    @PostAuthorize("returnObject.status != T(com.example.demo.common.enums.ProductStatus).DISCONTINUED || " +
            "hasAnyAuthority(T(com.example.demo.common.enums.Permission).ADMIN_ACCESS.getValue(), " +
            "T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Cacheable(value = "product", key = "#id")
    public ProductResponse findById(Long id) {
        Product product = productRepository.findDetailsById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "Product", id)));

        return new ProductResponse(product);
    }

    @PreAuthorize("hasAuthority(T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Transactional
    public long create(CreateProductRequest request) throws IOException {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategories(new HashSet<>(categoryRepository.findAllById(request.getCategoryIds())));

        addImages(product, request.getImages());

        productRepository.save(product);
        return product.getId();
    }

    @PreAuthorize("hasAuthority(T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Cacheable(value = "productEdit", key = "#id")
    public ProductEditResponse findEditById(Long id) {
        Product product = findEditByIdHelper(id);

        return new ProductEditResponse(product);
    }

    @PreAuthorize("hasAuthority(T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Caching(evict = {
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "productEdit", key = "#id")
    })
    @Transactional
    public void update(Long id, UpdateProductRequest request) throws IOException {
        Product product = findEditByIdHelper(id);

        if (request.getImages() == null) {
            request.setImages(new HashSet<>());
        }

        if (request.getKeptImageIds() == null) {
            request.setKeptImageIds(new HashSet<>());
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategories(new HashSet<>(categoryRepository.findAllById(request.getCategoryIds())));

        Set<Long> existingImageIds = product.getImages()
                .stream()
                .map(ProductImage::getId)
                .collect(Collectors.toSet());

        if (!existingImageIds.containsAll(request.getKeptImageIds())) {
            throw new IllegalArgumentException("Some images do not belong to this product");
        }

        product.getImages().removeIf(
                image -> !request.getKeptImageIds().contains(image.getId())
        );

        addImages(product, request.getImages());

        productRepository.save(product);
    }

    @PreAuthorize("hasAuthority(T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Caching(evict = {
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "productEdit", key = "#id")
    })
    @Transactional
    public void delete(Long id) {
        Product product = findByIdHelper(id);
        product.setStatus(DISCONTINUED);
        product.setDeleted(true);
        productRepository.save(product);
    }

    // --helpers --
    public Product findByIdHelper(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "Product", id)));
    }

    private Product findEditByIdHelper(Long id) {
        return productRepository.findEditById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "Product", id)));
    }

    public void decreaseForOrder(Product product, int quantity) {
        checkActive(product);

        if (product.getQuantity() < quantity) {
            throw new ValidationException("Product quantity is not available for product id: " + product.getId());
        }

        product.setQuantity(product.getQuantity() - quantity);
        productRepository.save(product);
    }

    public Product updateForStock(CreateStockItemRequest itemRequest) {
        Product product = findByIdHelper(itemRequest.productId());
        checkActive(product);

        product.setQuantity(product.getQuantity() + itemRequest.quantity());

        if (product.getStatus() == ProductStatus.COMING_SOON) {
            product.setStatus(ProductStatus.AVAILABLE);
        }

        return product;
    }

    private static String getNameFilter(String name) {
        return (isNull(name)) ? null : "%" + name + "%";
    }

    private void addImages(Product product, Set<MultipartFile> images) throws IOException {
        if (isEmpty(images)) {
            return;
        }

        for (MultipartFile imageFile : images) {
            if (fileExists(imageFile)) {
                String imageUrl = fileStorageService.uploadFile(imageFile);

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
