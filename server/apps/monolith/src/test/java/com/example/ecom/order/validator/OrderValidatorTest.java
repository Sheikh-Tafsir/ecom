package com.example.ecom.order.validator;

import com.example.ecom.order.dto.CreateOrderItemRequest;
import com.example.ecom.order.dto.CreateOrderRequest;
import com.example.ecom.order.enums.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OrderValidatorTest {

    @InjectMocks
    private OrderValidator orderValidator;

    private Errors errors;

    @BeforeEach
    void setUp() {
        // Will create fresh Errors per test
    }

    @Test
    @DisplayName("Valid order with unique product IDs passes validation")
    void validate_uniqueProducts_noErrors() {
        UUID product1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID product2 = UUID.fromString("22222222-2222-2222-2222-222222222222");

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(
                        new CreateOrderItemRequest(product1, 2),
                        new CreateOrderItemRequest(product2, 1)
                ),
                "John Doe",
                "01712345678",
                "123 Main Street, Dhaka",
                PaymentMethod.CASH_ON_DELIVERY
        );

        errors = new BeanPropertyBindingResult(request, "createOrderRequest");
        orderValidator.validate(request, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("Order with duplicate product IDs adds error")
    void validate_duplicateProducts_addsError() {
        UUID sameProduct = UUID.fromString("33333333-3333-3333-3333-333333333333");

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(
                        new CreateOrderItemRequest(sameProduct, 2),
                        new CreateOrderItemRequest(sameProduct, 3)
                ),
                "Jane Doe",
                "01898765432",
                "456 Second Avenue, Chittagong",
                PaymentMethod.BKASH
        );

        errors = new BeanPropertyBindingResult(request, "createOrderRequest");
        orderValidator.validate(request, errors);

        assertTrue(errors.hasFieldErrors("items"));
        assertEquals("Duplicate product is not allowed", errors.getFieldError("items").getDefaultMessage());
    }

    @Test
    @DisplayName("Order with null/empty items list passes validation without errors")
    void validate_nullItems_noErrors() {
        CreateOrderRequest request = new CreateOrderRequest(
                null,
                "John Doe",
                "01712345678",
                "123 Main Street, Dhaka",
                PaymentMethod.CASH_ON_DELIVERY
        );

        errors = new BeanPropertyBindingResult(request, "createOrderRequest");
        orderValidator.validate(request, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("Order with empty items list passes validation without errors")
    void validate_emptyItems_noErrors() {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(),
                "John Doe",
                "01712345678",
                "123 Main Street, Dhaka",
                PaymentMethod.CASH_ON_DELIVERY
        );

        errors = new BeanPropertyBindingResult(request, "createOrderRequest");
        orderValidator.validate(request, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("Order with null items in list skips them without error")
    void validate_nullItemInList_skipsGracefully() {
        UUID product1 = UUID.fromString("44444444-4444-4444-4444-444444444444");

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderItemRequest(product1, 1)),
                "John Doe",
                "01712345678",
                "123 Main Street, Dhaka",
                PaymentMethod.CASH_ON_DELIVERY
        );

        errors = new BeanPropertyBindingResult(request, "createOrderRequest");
        orderValidator.validate(request, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("Order with item having null productId does not flag as duplicate")
    void validate_nullProductId_noDuplicateError() {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(
                        new CreateOrderItemRequest(null, 1),
                        new CreateOrderItemRequest(null, 2)
                ),
                "John Doe",
                "01712345678",
                "123 Main Street, Dhaka",
                PaymentMethod.CASH_ON_DELIVERY
        );

        errors = new BeanPropertyBindingResult(request, "createOrderRequest");
        orderValidator.validate(request, errors);

        assertFalse(errors.hasFieldErrors("items"));
    }
}
