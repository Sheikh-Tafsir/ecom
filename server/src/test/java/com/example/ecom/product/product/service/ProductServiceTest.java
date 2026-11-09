package com.example.ecom.product.product.service;

import com.example.ecom.common.enums.ProductStatus;
import com.example.ecom.common.model.Category;
import com.example.ecom.common.model.Product;
import com.example.ecom.common.model.ProductImage;
import com.example.ecom.common.service.MessageService;
import com.example.ecom.common.service.fileStorage.FileStorageService;
import com.example.ecom.product.category.repository.CategoryRepository;
import com.example.ecom.product.product.dto.*;
import com.example.ecom.product.product.repository.ProductRepository;
import com.example.ecom.product.stock.dto.CreateStockItemRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private MessageService messageService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ProductService productService;

    private static final UUID PRODUCT_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID CATEGORY_ID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");
    private static final UUID IMAGE_ID_1 = UUID.fromString("c3d4e5f6-a7b8-9012-cdef-123456789012");
    private static final UUID IMAGE_ID_2 = UUID.fromString("d4e5f6a7-b8c9-0123-defa-234567890123");

    private Product product;
    private Category category;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(PRODUCT_ID);
        product.setName("Premium Wireless Headphones");
        product.setDescription("High-quality noise-cancelling headphones");
        product.setPrice(new BigDecimal("149.99"));
        product.setQuantity(50);
        product.setStatus(ProductStatus.AVAILABLE);
        product.setRating(BigDecimal.ZERO);
        product.setReviewCount(0);
        product.setDeleted(false);
        product.setImages(new HashSet<>());
        product.setCategories(new HashSet<>());

        category = new Category();
        category.setId(CATEGORY_ID);
        category.setName("Electronics");
    }

    @Test
    @DisplayName("findById should return ProductResponse when product exists")
    void findById_productExists_returnsProductResponse() {
        when(productRepository.findDetailsById(PRODUCT_ID)).thenReturn(Optional.of(product));

        ProductResponse result = productService.findById(PRODUCT_ID);

        assertNotNull(result);
        assertEquals(PRODUCT_ID, result.getId());
        assertEquals("Premium Wireless Headphones", result.getName());
        verify(productRepository).findDetailsById(PRODUCT_ID);
    }

    @Test
    @DisplayName("findById should throw EntityNotFoundException when product not found")
    void findById_productNotFound_throwsEntityNotFoundException() {
        when(productRepository.findDetailsById(PRODUCT_ID)).thenReturn(Optional.empty());
        when(messageService.get(eq("error.entity.not.found"), eq("Product"), eq(PRODUCT_ID)))
                .thenReturn("Product not found");

        assertThrows(EntityNotFoundException.class, () -> productService.findById(PRODUCT_ID));
        verify(productRepository).findDetailsById(PRODUCT_ID);
        verify(messageService).get("error.entity.not.found", "Product", PRODUCT_ID);
    }

    @Test
    @DisplayName("findEditById should return ProductEditResponse when product exists")
    void findEditById_productExists_returnsProductEditResponse() {
        when(productRepository.findEditById(PRODUCT_ID)).thenReturn(Optional.of(product));

        ProductEditResponse result = productService.findEditById(PRODUCT_ID);

        assertNotNull(result);
        assertEquals(PRODUCT_ID, result.getId());
        assertEquals("Premium Wireless Headphones", result.getName());
        verify(productRepository).findEditById(PRODUCT_ID);
    }

    @Test
    @DisplayName("findEditById should throw EntityNotFoundException when product not found")
    void findEditById_productNotFound_throwsEntityNotFoundException() {
        when(productRepository.findEditById(PRODUCT_ID)).thenReturn(Optional.empty());
        when(messageService.get(eq("error.entity.not.found"), eq("Product"), eq(PRODUCT_ID)))
                .thenReturn("Product not found");

        assertThrows(EntityNotFoundException.class, () -> productService.findEditById(PRODUCT_ID));
        verify(productRepository).findEditById(PRODUCT_ID);
    }

    @Test
    @DisplayName("create should save product and return its ID")
    void create_validRequest_savesProductAndReturnsId() throws IOException {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("New Speaker");
        request.setDescription("Portable Bluetooth Speaker with deep bass");
        request.setPrice(new BigDecimal("79.99"));
        request.setCategoryIds(Set.of(CATEGORY_ID));
        request.setImages(new HashSet<>());

        when(categoryRepository.findAllById(Set.of(CATEGORY_ID))).thenReturn(List.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(PRODUCT_ID);
            return saved;
        });

        UUID result = productService.create(request);

        assertNotNull(result);
        verify(categoryRepository).findAllById(Set.of(CATEGORY_ID));
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("create should upload images and associate with product")
    void create_withImages_uploadsAndAssociatesImages() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);

        CreateProductRequest request = new CreateProductRequest();
        request.setName("Camera");
        request.setDescription("High resolution digital camera with zoom");
        request.setPrice(new BigDecimal("599.99"));
        request.setCategoryIds(Set.of(CATEGORY_ID));
        request.setImages(Set.of(mockFile));

        when(categoryRepository.findAllById(Set.of(CATEGORY_ID))).thenReturn(List.of(category));
        when(fileStorageService.uploadFile(mockFile)).thenReturn("https://cdn.example.com/camera.jpg");
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(PRODUCT_ID);
            return saved;
        });

        UUID result = productService.create(request);

        assertNotNull(result);
        verify(fileStorageService).uploadFile(mockFile);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("delete should set product status to DISCONTINUED and mark as deleted")
    void delete_existingProduct_setsDiscontinuedAndDeleted() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.delete(PRODUCT_ID);

        assertEquals(ProductStatus.DISCONTINUED, product.getStatus());
        assertTrue(product.getDeleted());
        verify(productRepository).findById(PRODUCT_ID);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("delete should throw EntityNotFoundException when product does not exist")
    void delete_nonExistingProduct_throwsEntityNotFoundException() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());
        when(messageService.get(eq("error.entity.not.found"), eq("Product"), eq(PRODUCT_ID)))
                .thenReturn("Product not found");

        assertThrows(EntityNotFoundException.class, () -> productService.delete(PRODUCT_ID));
        verify(productRepository).findById(PRODUCT_ID);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("findByIdHelper should return product when found")
    void findByIdHelper_productExists_returnsProduct() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        Product result = productService.findByIdHelper(PRODUCT_ID);

        assertNotNull(result);
        assertEquals(PRODUCT_ID, result.getId());
        verify(productRepository).findById(PRODUCT_ID);
    }

    @Test
    @DisplayName("findByIdHelper should throw EntityNotFoundException when not found")
    void findByIdHelper_productNotFound_throwsEntityNotFoundException() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());
        when(messageService.get(eq("error.entity.not.found"), eq("Product"), eq(PRODUCT_ID)))
                .thenReturn("Product not found");

        assertThrows(EntityNotFoundException.class, () -> productService.findByIdHelper(PRODUCT_ID));
        verify(productRepository).findById(PRODUCT_ID);
    }

    @Test
    @DisplayName("decreaseForOrder should decrease stock when product is active and stock is sufficient")
    void decreaseForOrder_activeProductSufficientStock_decreasesStock() {
        when(productRepository.decreaseStockIfAvailable(PRODUCT_ID, 5)).thenReturn(1);

        productService.decreaseForOrder(product, 5);

        verify(productRepository).decreaseStockIfAvailable(PRODUCT_ID, 5);
        verify(entityManager).refresh(product);
    }

    @Test
    @DisplayName("decreaseForOrder should throw ValidationException when stock is insufficient")
    void decreaseForOrder_insufficientStock_throwsValidationException() {
        when(productRepository.decreaseStockIfAvailable(PRODUCT_ID, 100)).thenReturn(0);

        assertThrows(ValidationException.class, () -> productService.decreaseForOrder(product, 100));
        verify(productRepository).decreaseStockIfAvailable(PRODUCT_ID, 100);
        verify(entityManager, never()).refresh(any());
    }

    @Test
    @DisplayName("decreaseForOrder should throw ValidationException when product is discontinued")
    void decreaseForOrder_discontinuedProduct_throwsValidationException() {
        product.setStatus(ProductStatus.DISCONTINUED);

        assertThrows(ValidationException.class, () -> productService.decreaseForOrder(product, 5));
        verify(productRepository, never()).decreaseStockIfAvailable(any(), anyInt());
    }

    @Test
    @DisplayName("decreaseForOrder should throw ValidationException when product is deleted")
    void decreaseForOrder_deletedProduct_throwsValidationException() {
        product.setDeleted(true);

        assertThrows(ValidationException.class, () -> productService.decreaseForOrder(product, 5));
        verify(productRepository, never()).decreaseStockIfAvailable(any(), anyInt());
    }

    @Test
    @DisplayName("updateForStock should increase product quantity and return updated product")
    void updateForStock_activeProduct_increasesQuantityAndReturns() {
        CreateStockItemRequest itemRequest = new CreateStockItemRequest(PRODUCT_ID, 20, new BigDecimal("50.00"));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product result = productService.updateForStock(itemRequest);

        assertNotNull(result);
        assertEquals(70, result.getQuantity());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("updateForStock should change COMING_SOON status to AVAILABLE when stock is added")
    void updateForStock_comingSoonProduct_setsStatusToAvailable() {
        product.setStatus(ProductStatus.COMING_SOON);
        product.setQuantity(0);
        CreateStockItemRequest itemRequest = new CreateStockItemRequest(PRODUCT_ID, 10, new BigDecimal("30.00"));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product result = productService.updateForStock(itemRequest);

        assertEquals(ProductStatus.AVAILABLE, result.getStatus());
        assertEquals(10, result.getQuantity());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("updateForStock should throw ValidationException when product is not active")
    void updateForStock_discontinuedProduct_throwsValidationException() {
        product.setStatus(ProductStatus.DISCONTINUED);
        CreateStockItemRequest itemRequest = new CreateStockItemRequest(PRODUCT_ID, 10, new BigDecimal("30.00"));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThrows(ValidationException.class, () -> productService.updateForStock(itemRequest));
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("increaseQuantity should call repository increaseStock and refresh entity")
    void increaseQuantity_validProduct_callsRepoAndRefreshes() {
        productService.increaseQuantity(product, 15);

        verify(productRepository).increaseStock(PRODUCT_ID, 15);
        verify(entityManager).refresh(product);
    }

    @Test
    @DisplayName("update should throw EntityNotFoundException when product not found")
    void update_productNotFound_throwsEntityNotFoundException() throws IOException {
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("Updated Name");
        request.setDescription("Updated description for the product");
        request.setPrice(new BigDecimal("199.99"));
        request.setCategoryIds(Set.of(CATEGORY_ID));

        when(productRepository.findEditById(PRODUCT_ID)).thenReturn(Optional.empty());
        when(messageService.get(eq("error.entity.not.found"), eq("Product"), eq(PRODUCT_ID)))
                .thenReturn("Product not found");

        assertThrows(EntityNotFoundException.class, () -> productService.update(PRODUCT_ID, request));
        verify(productRepository).findEditById(PRODUCT_ID);
    }

    @Test
    @DisplayName("update should throw IllegalArgumentException when kept image IDs don't belong to product")
    void update_invalidKeptImageIds_throwsIllegalArgumentException() throws IOException {
        ProductImage img1 = new ProductImage();
        img1.setId(IMAGE_ID_1);
        img1.setImage("https://cdn.example.com/img1.jpg");
        product.setImages(new HashSet<>(Set.of(img1)));

        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("Updated Name");
        request.setDescription("Updated description for the product");
        request.setPrice(new BigDecimal("199.99"));
        request.setCategoryIds(Set.of(CATEGORY_ID));
        request.setKeptImageIds(Set.of(IMAGE_ID_2));
        request.setImages(new HashSet<>());

        when(productRepository.findEditById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(categoryRepository.findAllById(Set.of(CATEGORY_ID))).thenReturn(List.of(category));

        assertThrows(IllegalArgumentException.class, () -> productService.update(PRODUCT_ID, request));
    }

    @Test
    @DisplayName("update should update product fields when valid request is provided")
    void update_validRequest_updatesProductFields() throws IOException {
        product.setImages(new HashSet<>());

        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("Updated Headphones");
        request.setDescription("Updated description for headphones");
        request.setPrice(new BigDecimal("199.99"));
        request.setCategoryIds(Set.of(CATEGORY_ID));
        request.setKeptImageIds(new HashSet<>());
        request.setImages(new HashSet<>());

        when(productRepository.findEditById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(categoryRepository.findAllById(Set.of(CATEGORY_ID))).thenReturn(List.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.update(PRODUCT_ID, request);

        assertEquals("Updated Headphones", product.getName());
        assertEquals(new BigDecimal("199.99"), product.getPrice());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("update should handle null images and keptImageIds by setting them to empty sets")
    void update_nullImagesAndKeptImageIds_handledGracefully() throws IOException {
        product.setImages(new HashSet<>());

        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("Updated Headphones");
        request.setDescription("Updated description for headphones");
        request.setPrice(new BigDecimal("199.99"));
        request.setCategoryIds(Set.of(CATEGORY_ID));
        request.setImages(null);
        request.setKeptImageIds(null);

        when(productRepository.findEditById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(categoryRepository.findAllById(Set.of(CATEGORY_ID))).thenReturn(List.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.update(PRODUCT_ID, request);

        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("search should return mapped product list responses")
    void search_withName_returnsMappedResults() {
        when(productRepository.searchByName(eq("%Head%"), eq(ProductStatus.DISCONTINUED), any()))
                .thenReturn(List.of(product));

        List<ProductListResponse> result = productService.search("Head");

        assertEquals(1, result.size());
        assertEquals("Premium Wireless Headphones", result.get(0).getName());
        verify(productRepository).searchByName(eq("%Head%"), eq(ProductStatus.DISCONTINUED), any());
    }

    @Test
    @DisplayName("search with null name should pass null filter to repository")
    void search_withNullName_passesNullFilter() {
        when(productRepository.searchByName(isNull(), eq(ProductStatus.DISCONTINUED), any()))
                .thenReturn(List.of());

        List<ProductListResponse> result = productService.search(null);

        assertTrue(result.isEmpty());
        verify(productRepository).searchByName(isNull(), eq(ProductStatus.DISCONTINUED), any());
    }
}
