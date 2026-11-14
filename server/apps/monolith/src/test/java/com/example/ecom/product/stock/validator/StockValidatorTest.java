package com.example.ecom.product.stock.validator;

import com.example.ecom.product.stock.dto.CreateStockItemRequest;
import com.example.ecom.product.stock.dto.CreateStockRequest;
import com.example.ecom.product.stock.dto.UpdateStockItemRequest;
import com.example.ecom.product.stock.dto.UpdateStockRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.validation.Errors;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StockValidatorTest {

    @InjectMocks
    private StockValidator stockValidator;

    private static final UUID PRODUCT_ID_1 = UUID.fromString("a1a1a1a1-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID_2 = UUID.fromString("b2b2b2b2-2222-2222-2222-222222222222");
    private static final UUID ITEM_ID_1 = UUID.fromString("c3c3c3c3-3333-3333-3333-333333333333");
    private static final UUID ITEM_ID_2 = UUID.fromString("d4d4d4d4-4444-4444-4444-444444444444");

    @Test
    @DisplayName("validateCreate should pass when items have unique products and valid prices")
    void validateCreate_validItems_noErrors() {
        CreateStockItemRequest item1 = new CreateStockItemRequest(PRODUCT_ID_1, 10, new BigDecimal("5.00"));
        CreateStockItemRequest item2 = new CreateStockItemRequest(PRODUCT_ID_2, 20, new BigDecimal("10.00"));
        CreateStockRequest request = new CreateStockRequest(List.of(item1, item2));
        Errors errors = new DirectFieldBindingResult(request, "request");

        stockValidator.validateCreate(request, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("validateCreate should reject when items have duplicate product IDs")
    void validateCreate_duplicateProductIds_rejectsWithError() {
        CreateStockItemRequest item1 = new CreateStockItemRequest(PRODUCT_ID_1, 10, new BigDecimal("5.00"));
        CreateStockItemRequest item2 = new CreateStockItemRequest(PRODUCT_ID_1, 15, new BigDecimal("6.00"));
        CreateStockRequest request = new CreateStockRequest(List.of(item1, item2));
        Errors errors = new DirectFieldBindingResult(request, "request");

        stockValidator.validateCreate(request, errors);

        assertTrue(errors.hasErrors());
        assertNotNull(errors.getFieldError("items"));
    }

    @Test
    @DisplayName("validateCreate should reject when purchase price is negative")
    void validateCreate_negativePurchasePrice_rejectsWithError() {
        CreateStockItemRequest item = new CreateStockItemRequest(PRODUCT_ID_1, 10, new BigDecimal("-5.00"));
        CreateStockRequest request = new CreateStockRequest(List.of(item));
        Errors errors = new org.springframework.validation.MapBindingResult(new java.util.HashMap<>(), "request");

        stockValidator.validateCreate(request, errors);

        assertTrue(errors.hasErrors());
    }

    @Test
    @DisplayName("validateCreate should pass when purchase price is zero")
    void validateCreate_zeroPurchasePrice_noErrors() {
        CreateStockItemRequest item = new CreateStockItemRequest(PRODUCT_ID_1, 10, BigDecimal.ZERO);
        CreateStockRequest request = new CreateStockRequest(List.of(item));
        Errors errors = new DirectFieldBindingResult(request, "request");

        stockValidator.validateCreate(request, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("validateCreate should handle null items list gracefully")
    void validateCreate_nullItems_noErrors() {
        CreateStockRequest request = new CreateStockRequest(null);
        Errors errors = new DirectFieldBindingResult(request, "request");

        stockValidator.validateCreate(request, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("validateCreate should pass when purchase price is null")
    void validateCreate_nullPurchasePrice_noErrors() {
        CreateStockItemRequest item = new CreateStockItemRequest(PRODUCT_ID_1, 10, null);
        CreateStockRequest request = new CreateStockRequest(List.of(item));
        Errors errors = new DirectFieldBindingResult(request, "request");

        stockValidator.validateCreate(request, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("validateUpdate should pass when items have unique IDs and valid prices")
    void validateUpdate_validItems_noErrors() {
        UpdateStockItemRequest item1 = new UpdateStockItemRequest(ITEM_ID_1, 10, new BigDecimal("5.00"));
        UpdateStockItemRequest item2 = new UpdateStockItemRequest(ITEM_ID_2, 20, new BigDecimal("10.00"));
        UpdateStockRequest request = new UpdateStockRequest(List.of(item1, item2));
        Errors errors = new DirectFieldBindingResult(request, "request");

        stockValidator.validateUpdate(request, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("validateUpdate should reject when items have duplicate IDs")
    void validateUpdate_duplicateItemIds_rejectsWithError() {
        UpdateStockItemRequest item1 = new UpdateStockItemRequest(ITEM_ID_1, 10, new BigDecimal("5.00"));
        UpdateStockItemRequest item2 = new UpdateStockItemRequest(ITEM_ID_1, 15, new BigDecimal("6.00"));
        UpdateStockRequest request = new UpdateStockRequest(List.of(item1, item2));
        Errors errors = new DirectFieldBindingResult(request, "request");

        stockValidator.validateUpdate(request, errors);

        assertTrue(errors.hasErrors());
        assertNotNull(errors.getFieldError("items"));
    }

    @Test
    @DisplayName("validateUpdate should reject when item ID is null")
    void validateUpdate_nullItemId_rejectsWithError() {
        UpdateStockItemRequest item = new UpdateStockItemRequest(null, 10, new BigDecimal("5.00"));
        UpdateStockRequest request = new UpdateStockRequest(List.of(item));
        Errors errors = new DirectFieldBindingResult(request, "request");

        stockValidator.validateUpdate(request, errors);

        assertTrue(errors.hasErrors());
    }

    @Test
    @DisplayName("validateUpdate should reject when purchase price is negative")
    void validateUpdate_negativePurchasePrice_rejectsWithError() {
        UpdateStockItemRequest item = new UpdateStockItemRequest(ITEM_ID_1, 10, new BigDecimal("-3.00"));
        UpdateStockRequest request = new UpdateStockRequest(List.of(item));
        Errors errors = new org.springframework.validation.MapBindingResult(new java.util.HashMap<>(), "request");

        stockValidator.validateUpdate(request, errors);

        assertTrue(errors.hasErrors());
    }

    @Test
    @DisplayName("validateUpdate should handle null items list gracefully")
    void validateUpdate_nullItems_noErrors() {
        UpdateStockRequest request = new UpdateStockRequest(null);
        Errors errors = new DirectFieldBindingResult(request, "request");

        stockValidator.validateUpdate(request, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("validateCreateItem should reject negative purchase price")
    void validateCreateItem_negativePurchasePrice_rejectsWithError() {
        CreateStockItemRequest item = new CreateStockItemRequest(PRODUCT_ID_1, 5, new BigDecimal("-1.50"));
        Errors errors = new DirectFieldBindingResult(item, "request");

        stockValidator.validateCreateItem(item, errors);

        assertTrue(errors.hasErrors());
    }

    @Test
    @DisplayName("validateCreateItem should pass with positive purchase price")
    void validateCreateItem_positivePurchasePrice_noErrors() {
        CreateStockItemRequest item = new CreateStockItemRequest(PRODUCT_ID_1, 5, new BigDecimal("25.00"));
        Errors errors = new DirectFieldBindingResult(item, "request");

        stockValidator.validateCreateItem(item, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("validateUpdateItem should reject negative purchase price")
    void validateUpdateItem_negativePurchasePrice_rejectsWithError() {
        UpdateStockItemRequest item = new UpdateStockItemRequest(ITEM_ID_1, 5, new BigDecimal("-0.01"));
        Errors errors = new DirectFieldBindingResult(item, "request");

        stockValidator.validateUpdateItem(item, errors);

        assertTrue(errors.hasErrors());
    }

    @Test
    @DisplayName("validateUpdateItem should pass with positive purchase price")
    void validateUpdateItem_positivePurchasePrice_noErrors() {
        UpdateStockItemRequest item = new UpdateStockItemRequest(ITEM_ID_1, 5, new BigDecimal("99.99"));
        Errors errors = new DirectFieldBindingResult(item, "request");

        stockValidator.validateUpdateItem(item, errors);

        assertFalse(errors.hasErrors());
    }
}
