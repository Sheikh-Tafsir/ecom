package com.example.ecom.product.product.adapter;

import com.example.ecom.common.client.product.ProductBasicDto;
import com.example.ecom.product.product.model.Product;
import com.example.ecom.product.product.model.ProductImage;
import com.example.ecom.product.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductClientAdapterTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductClientAdapter productClientAdapter;

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    @DisplayName("findProductById should deterministically select image with smallest UUID when multiple images exist")
    void findProductById_multipleImages_selectsDeterministicImageSortedByUuid() {
        UUID imgId1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID imgId2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID imgId3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

        ProductImage img1 = new ProductImage(imgId1, "https://example.com/img1.jpg", null);
        ProductImage img2 = new ProductImage(imgId2, "https://example.com/img2.jpg", null);
        ProductImage img3 = new ProductImage(imgId3, "https://example.com/img3.jpg", null);

        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setName("Mechanical Keyboard");
        product.setPrice(new BigDecimal("99.99"));
        product.setQuantity(25);
        product.setImages(Set.of(img3, img1, img2));

        when(productService.findByIdHelper(PRODUCT_ID)).thenReturn(product);

        ProductBasicDto result = productClientAdapter.findProductById(PRODUCT_ID);

        assertNotNull(result);
        assertEquals(PRODUCT_ID, result.id());
        assertEquals("Mechanical Keyboard", result.name());
        assertEquals("https://example.com/img1.jpg", result.firstImage());
        assertEquals(new BigDecimal("99.99"), result.price());
        assertEquals(25, result.quantity());
        verify(productService).findByIdHelper(PRODUCT_ID);
    }

    @Test
    @DisplayName("findProductById should return null firstImage when images set is empty")
    void findProductById_emptyImages_returnsNullFirstImage() {
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setName("Mouse Pad");
        product.setPrice(new BigDecimal("19.99"));
        product.setQuantity(50);
        product.setImages(Set.of());

        when(productService.findByIdHelper(PRODUCT_ID)).thenReturn(product);

        ProductBasicDto result = productClientAdapter.findProductById(PRODUCT_ID);

        assertNotNull(result);
        assertEquals(PRODUCT_ID, result.id());
        assertEquals("Mouse Pad", result.name());
        assertNull(result.firstImage());
        assertEquals(new BigDecimal("19.99"), result.price());
        assertEquals(50, result.quantity());
    }

    @Test
    @DisplayName("findProductById should return null firstImage when images set is null")
    void findProductById_nullImages_returnsNullFirstImage() {
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setName("Mouse Pad");
        product.setPrice(new BigDecimal("19.99"));
        product.setQuantity(50);
        product.setImages(null);

        when(productService.findByIdHelper(PRODUCT_ID)).thenReturn(product);

        ProductBasicDto result = productClientAdapter.findProductById(PRODUCT_ID);

        assertNotNull(result);
        assertEquals(PRODUCT_ID, result.id());
        assertNull(result.firstImage());
    }

    @Test
    @DisplayName("hasStock should return true when available quantity is greater than or equal to requested")
    void hasStock_sufficientStock_returnsTrue() {
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setQuantity(10);

        when(productService.findByIdHelper(PRODUCT_ID)).thenReturn(product);

        assertTrue(productClientAdapter.hasStock(PRODUCT_ID, 5));
        assertTrue(productClientAdapter.hasStock(PRODUCT_ID, 10));
    }

    @Test
    @DisplayName("hasStock should return false when available quantity is less than requested")
    void hasStock_insufficientStock_returnsFalse() {
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setQuantity(4);

        when(productService.findByIdHelper(PRODUCT_ID)).thenReturn(product);

        assertFalse(productClientAdapter.hasStock(PRODUCT_ID, 5));
    }

    @Test
    @DisplayName("reserveStock should delegate to productService.decreaseForOrder")
    void reserveStock_delegatesToProductService() {
        productClientAdapter.reserveStock(PRODUCT_ID, 3);

        verify(productService).decreaseForOrder(PRODUCT_ID, 3);
    }

    @Test
    @DisplayName("releaseStock should delegate to productService.increaseQuantity")
    void releaseStock_delegatesToProductService() {
        productClientAdapter.releaseStock(PRODUCT_ID, 3);

        verify(productService).increaseQuantity(PRODUCT_ID, 3);
    }
}
