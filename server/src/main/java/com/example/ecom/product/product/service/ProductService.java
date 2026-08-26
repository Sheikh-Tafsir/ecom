package com.example.ecom.product.product.service;

import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.common.dto.DateRangeDto;
import com.example.ecom.common.enums.ProductStatus;
import com.example.ecom.common.model.Product;
import com.example.ecom.common.model.ProductImage;
import com.example.ecom.common.service.fileStorage.FileStorageService;
import com.example.ecom.common.service.MessageService;
import com.example.ecom.common.utils.FileUtils;
import com.example.ecom.product.product.dto.*;
import com.example.ecom.product.category.repository.CategoryRepository;
import com.example.ecom.product.product.repository.ProductRepository;
import com.example.ecom.product.stock.dto.CreateStockItemRequest;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.example.ecom.common.utils.CacheConstants.CACHE_PRODUCTS;
import static com.example.ecom.common.utils.CacheConstants.CACHE_PRODUCTS_EDIT;
import static com.example.ecom.common.enums.Permission.ADMIN_ACCESS;
import static com.example.ecom.common.enums.Permission.SUPER_ADMIN_ACCESS;
import static com.example.ecom.common.enums.ProductStatus.DISCONTINUED;
import static com.example.ecom.common.utils.DateUtils.resolveDates;
import static com.example.ecom.common.utils.SecurityUtil.hasPermission;
import static com.example.ecom.common.utils.Utils.*;

@Slf4j
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

    @PostAuthorize("returnObject.status != T(com.example.ecom.common.enums.ProductStatus).DISCONTINUED || " +
            "hasAnyAuthority(T(com.example.ecom.common.enums.Permission).ADMIN_ACCESS.getValue(), " +
            "T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Cacheable(value = CACHE_PRODUCTS, key = "#id")
    public ProductResponse findById(Long id) {
        Product product = productRepository.findDetailsById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "Product", id)));

        return new ProductResponse(product);
    }

    @PreAuthorize("hasAuthority(T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Transactional
    public long create(CreateProductRequest request) throws IOException {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategories(new HashSet<>(categoryRepository.findAllById(request.getCategoryIds())));

        Set<String> uploadedImageUrls = new HashSet<>();
        registerImageCleanup(uploadedImageUrls);

        addImages(product, request.getImages(), uploadedImageUrls);
        productRepository.save(product);
        return product.getId();
    }

    @PreAuthorize("hasAuthority(T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Cacheable(value = CACHE_PRODUCTS_EDIT, key = "#id")
    public ProductEditResponse findEditById(Long id) {
        Product product = findEditByIdHelper(id);

        return new ProductEditResponse(product);
    }

    @PreAuthorize("hasAuthority(T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Caching(evict = {
            @CacheEvict(value = CACHE_PRODUCTS, key = "#id"),
            @CacheEvict(value = CACHE_PRODUCTS_EDIT, key = "#id")
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

        Set<ProductImage> removedImages = product.getImages()
                .stream()
                .filter(image -> !request.getKeptImageIds().contains(image.getId()))
                .collect(Collectors.toSet());

        Set<String> uploadedImageUrls = new HashSet<>();
        registerImageCleanup(uploadedImageUrls);

        addImages(product, request.getImages(), uploadedImageUrls);
        product.getImages().removeAll(removedImages);
        productRepository.save(product);

        // Only delete from storage if DB transaction succeeds
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    removedImages.forEach(image -> fileStorageService.deleteFileAsync(image.getImage()));
                }
            });
        } else {
            removedImages.forEach(image -> fileStorageService.deleteFileAsync(image.getImage()));
        }
    }

    @PreAuthorize("hasAuthority(T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Caching(evict = {
            @CacheEvict(value = CACHE_PRODUCTS, key = "#id"),
            @CacheEvict(value = CACHE_PRODUCTS_EDIT, key = "#id")
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

    @Caching(evict = {
            @CacheEvict(value = CACHE_PRODUCTS, key = "#product.id"),
            @CacheEvict(value = CACHE_PRODUCTS_EDIT, key = "#product.id")
    })
    public void decreaseForOrder(Product product, int quantity) {
        checkActive(product);

        int updatedRows = productRepository.decreaseStockIfAvailable(product.getId(), quantity);
        if (updatedRows == 0) {
            throw new ValidationException("Product stock is insufficient or unavailable for product id: " + product.getId());
        }

        product.setQuantity(product.getQuantity() - quantity);
    }

    @Caching(evict = {
            @CacheEvict(value = CACHE_PRODUCTS, key = "#itemRequest.productId()"),
            @CacheEvict(value = CACHE_PRODUCTS_EDIT, key = "#itemRequest.productId()")
    })
    public Product updateForStock(CreateStockItemRequest itemRequest) {
        Product product = findByIdHelper(itemRequest.productId());
        checkActive(product);

        product.setQuantity(product.getQuantity() + itemRequest.quantity());

        if (product.getStatus() == ProductStatus.COMING_SOON) {
            product.setStatus(ProductStatus.AVAILABLE);
        }

        productRepository.save(product);
        return product;
    }

    @Caching(evict = {
            @CacheEvict(value = CACHE_PRODUCTS, key = "#product.id"),
            @CacheEvict(value = CACHE_PRODUCTS_EDIT, key = "#product.id")
    })
    public void increaseQuantity(Product product, int quantityChange) {
        productRepository.increaseStock(product.getId(), quantityChange);
        product.setQuantity(product.getQuantity() + quantityChange);
    }

    private void registerImageCleanup(Set<String> uploadedImageUrls) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        log.info("Transaction rolled back, cleaning up uploaded images: {}", uploadedImageUrls);
                        uploadedImageUrls.forEach(fileStorageService::deleteFileAsync);
                    }
                }
            });
        }
    }

    private static String getNameFilter(String name) {
        return (isNull(name)) ? null : "%" + name + "%";
    }

    private void addImages(Product product, Set<MultipartFile> images, Set<String> uploadedUrls) {
        if (images == null || images.isEmpty()) {
            return;
        }

        for (MultipartFile image : images) {
            if (FileUtils.fileExists(image)) {
                try {
                    String url = fileStorageService.uploadFile(image);
                    uploadedUrls.add(url);
                    ProductImage productImage = new ProductImage();
                    productImage.setImage(url);
                    product.addImage(productImage);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to upload image: " + image.getOriginalFilename(), e);
                }
            }
        }
    }

    private void checkActive(Product product) {
        if (product.isNotActive()) {
            throw new ValidationException("Product is not active: " + product.getId());
        }
    }
}
