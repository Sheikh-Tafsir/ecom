package com.example.ecom.product.product.dto;

import com.example.ecom.common.enums.ProductStatus;
import com.example.ecom.common.model.Category;
import com.example.ecom.common.model.Product;
import com.example.ecom.common.model.ProductImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductDtoTest {

    private UUID productId;
    private Product product;

    @BeforeEach
    void setUp() {
        productId = UUID.fromString("01927d6f-097f-7afa-9ad3-960718293041");
        product = new Product();
        product.setId(productId);
        product.setName("Mechanical Keyboard");
        product.setDescription("Cherry MX Blue switches with RGB lighting");
        product.setPrice(new BigDecimal("129.99"));
        product.setQuantity(50);
        product.setStatus(ProductStatus.AVAILABLE);
        product.setRating(new BigDecimal("4.5"));
        product.setReviewCount(120);

        ProductImage image = new ProductImage();
        image.setId(UUID.fromString("01927d6f-1a8f-7bfa-8ad4-071829304152"));
        image.setImage("https://cdn.example.com/keyboard.jpg");
        product.addImage(image);

        Category category = new Category();
        category.setId(UUID.fromString("01927d6f-2b9f-7cfa-7ad5-182930415263"));
        category.setName("Peripherals");
        product.setCategories(Set.of(category));
    }

    @Nested
    @DisplayName("ProductImageResponse Tests")
    class ProductImageResponseTest {

        @Test
        @DisplayName("Construction from ProductImage entity maps fields")
        void constructor_fromEntity_setsFields() {
            ProductImage image = product.getImages().iterator().next();
            ProductImageResponse response = new ProductImageResponse(image);

            assertEquals(image.getId(), response.getId());
            assertEquals("https://cdn.example.com/keyboard.jpg", response.getImage());
        }

        @Test
        @DisplayName("No-args constructor creates empty response")
        void noArgsConstructor_createsEmpty() {
            ProductImageResponse response = new ProductImageResponse();
            assertNull(response.getId());
            assertNull(response.getImage());
        }

        @Test
        @DisplayName("All-args constructor sets fields")
        void allArgsConstructor_setsFields() {
            UUID id = UUID.fromString("01927d6f-3caf-7dfa-6ad6-293041526374");
            ProductImageResponse response = new ProductImageResponse(id, "img.jpg");

            assertEquals(id, response.getId());
            assertEquals("img.jpg", response.getImage());
        }
    }

    @Nested
    @DisplayName("ProductListResponse Tests")
    class ProductListResponseTest {

        @Test
        @DisplayName("Construction from Product entity maps all fields")
        void constructor_fromEntity_setsAllFields() {
            product.setFirstImage("https://cdn.example.com/keyboard.jpg");

            ProductListResponse response = new ProductListResponse(product);

            assertEquals(productId, response.getId());
            assertEquals("Mechanical Keyboard", response.getName());
            assertEquals(new BigDecimal("129.99"), response.getPrice());
            assertEquals(new BigDecimal("4.5"), response.getRating());
            assertEquals(50, response.getQuantity());
            assertEquals("https://cdn.example.com/keyboard.jpg", response.getImage());
            assertNotNull(response.getCategories());
            assertTrue(response.getCategories().contains("Peripherals"));
        }

        @Test
        @DisplayName("No-args constructor creates empty response")
        void noArgsConstructor_createsEmpty() {
            ProductListResponse response = new ProductListResponse();
            assertNull(response.getId());
            assertNull(response.getName());
        }
    }

    @Nested
    @DisplayName("ProductResponse Tests")
    class ProductResponseTest {

        @Test
        @DisplayName("Construction from Product entity maps all fields including images and categories")
        void constructor_fromEntity_setsAllFields() {
            ProductResponse response = new ProductResponse(product);

            assertEquals(productId, response.getId());
            assertEquals("Mechanical Keyboard", response.getName());
            assertEquals("Cherry MX Blue switches with RGB lighting", response.getDescription());
            assertEquals(new BigDecimal("129.99"), response.getPrice());
            assertEquals(50, response.getQuantity());
            assertEquals(ProductStatus.AVAILABLE, response.getStatus());
            assertEquals(new BigDecimal("4.5"), response.getRating());
            assertEquals(120, response.getReviewCount());
            assertEquals(1, response.getImages().size());
            assertEquals(1, response.getCategories().size());
        }

        @Test
        @DisplayName("No-args constructor creates empty response")
        void noArgsConstructor_createsEmpty() {
            ProductResponse response = new ProductResponse();
            assertNull(response.getId());
            assertNull(response.getStatus());
        }
    }

    @Nested
    @DisplayName("ProductEditResponse Tests")
    class ProductEditResponseTest {

        @Test
        @DisplayName("Construction from Product entity maps editing-relevant fields")
        void constructor_fromEntity_setsFields() {
            ProductEditResponse response = new ProductEditResponse(product);

            assertEquals(productId, response.getId());
            assertEquals("Mechanical Keyboard", response.getName());
            assertEquals("Cherry MX Blue switches with RGB lighting", response.getDescription());
            assertEquals(new BigDecimal("129.99"), response.getPrice());
            assertEquals(1, response.getImages().size());
            assertEquals(1, response.getCategories().size());
        }

        @Test
        @DisplayName("No-args constructor creates empty response")
        void noArgsConstructor_createsEmpty() {
            ProductEditResponse response = new ProductEditResponse();
            assertNull(response.getId());
        }
    }

    @Nested
    @DisplayName("CreateProductRequest Tests")
    class CreateProductRequestTest {

        @Test
        @DisplayName("Setters and getters work correctly")
        void settersGetters_allFields_workCorrectly() {
            CreateProductRequest request = new CreateProductRequest();
            request.setName("Gaming Mouse");
            request.setDescription("High precision gaming mouse");
            request.setPrice(new BigDecimal("59.99"));
            request.setCategoryIds(Set.of(UUID.randomUUID()));

            assertEquals("Gaming Mouse", request.getName());
            assertEquals("High precision gaming mouse", request.getDescription());
            assertEquals(new BigDecimal("59.99"), request.getPrice());
            assertEquals(1, request.getCategoryIds().size());
        }
    }

    @Nested
    @DisplayName("UpdateProductRequest Tests")
    class UpdateProductRequestTest {

        @Test
        @DisplayName("Setters and getters work correctly")
        void settersGetters_allFields_workCorrectly() {
            UpdateProductRequest request = new UpdateProductRequest();
            UUID keptImageId = UUID.fromString("01927d6f-4dbf-7efa-5ad7-304152637485");
            UUID catId = UUID.fromString("01927d6f-5ecf-7ffa-4ad8-415263748596");

            request.setName("Updated Mouse");
            request.setDescription("Updated description");
            request.setPrice(new BigDecimal("69.99"));
            request.setKeptImageIds(Set.of(keptImageId));
            request.setCategoryIds(Set.of(catId));

            assertEquals("Updated Mouse", request.getName());
            assertEquals("Updated description", request.getDescription());
            assertEquals(new BigDecimal("69.99"), request.getPrice());
            assertTrue(request.getKeptImageIds().contains(keptImageId));
            assertTrue(request.getCategoryIds().contains(catId));
        }
    }
}
