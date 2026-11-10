package com.example.ecom.product.sale.dto;

import com.example.ecom.order.model.Order;
import com.example.ecom.product.product.model.Product;
import com.example.ecom.product.product.model.ProductImage;
import com.example.ecom.product.sale.model.Sale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SaleResponseTest {

    @Test
    @DisplayName("Construction from Sale entity with product and images maps all fields")
    void constructor_fromEntityWithProduct_setsAllFields() {
        UUID saleId = UUID.fromString("01927d77-097f-7afa-9aee-637485960718");
        UUID productId = UUID.fromString("01927d77-1a8f-7bfa-8aef-748596071829");
        Instant now = Instant.parse("2025-10-15T12:00:00Z");

        Product product = new Product();
        product.setId(productId);
        product.setName("Premium Headphones");

        ProductImage image = new ProductImage();
        image.setImage("https://cdn.example.com/headphones.jpg");
        product.addImage(image);

        Sale sale = new Sale();
        sale.setId(saleId);
        sale.setProduct(product);
        sale.setQuantity(3);
        sale.setProfit(new BigDecimal("450.00"));
        sale.setCreatedAt(now);

        SaleResponse response = new SaleResponse(sale);

        assertEquals(saleId, response.getId());
        assertEquals(productId, response.getProductId());
        assertEquals("Premium Headphones", response.getProductName());
        assertEquals("https://cdn.example.com/headphones.jpg", response.getProductImage());
        assertEquals(new BigDecimal("450.00"), response.getProfit());
        assertEquals(3, response.getQuantity());
        assertEquals(now, response.getCreatedAt());
    }

    @Test
    @DisplayName("Construction from Sale entity without product handles nulls")
    void constructor_fromEntityWithoutProduct_handlesNulls() {
        Sale sale = new Sale();
        sale.setId(UUID.fromString("01927d77-2b9f-7cfa-7af0-859607182930"));
        sale.setProduct(null);
        sale.setQuantity(1);
        sale.setProfit(new BigDecimal("50.00"));

        SaleResponse response = new SaleResponse(sale);

        assertNull(response.getProductId());
        assertNull(response.getProductName());
        assertNull(response.getProductImage());
    }

    @Test
    @DisplayName("Construction from Sale entity with product but no images sets null productImage")
    void constructor_fromEntityNoImages_nullProductImage() {
        Product product = new Product();
        product.setId(UUID.fromString("01927d77-3caf-7dfa-6af1-960718293041"));
        product.setName("Simple Widget");

        Sale sale = new Sale();
        sale.setId(UUID.fromString("01927d77-4dbf-7efa-5af2-071829304152"));
        sale.setProduct(product);
        sale.setQuantity(10);
        sale.setProfit(new BigDecimal("100.00"));

        SaleResponse response = new SaleResponse(sale);

        assertNull(response.getProductImage());
    }

    @Test
    @DisplayName("No-args constructor creates empty response")
    void noArgsConstructor_createsEmpty() {
        SaleResponse response = new SaleResponse();
        assertNull(response.getId());
        assertNull(response.getProductId());
    }
}
