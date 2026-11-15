package com.example.ecom.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import static org.junit.jupiter.api.Assertions.*;

class CustomExceptionClassesTest {

    // --- InvalidAccessTokenException ---

    @Test
    @DisplayName("InvalidAccessTokenException stores the provided message")
    void invalidAccessTokenException_message_isPreserved() {
        String message = "Token expired at 2024-06-15T10:30:00Z";
        InvalidAccessTokenException ex = new InvalidAccessTokenException(message);

        assertEquals(message, ex.getMessage());
        assertInstanceOf(RuntimeException.class, ex);
    }

    // --- InvalidRefreshTokenException ---

    @Test
    @DisplayName("InvalidRefreshTokenException stores the provided message")
    void invalidRefreshTokenException_message_isPreserved() {
        String message = "Refresh token has been revoked";
        InvalidRefreshTokenException ex = new InvalidRefreshTokenException(message);

        assertEquals(message, ex.getMessage());
        assertInstanceOf(RuntimeException.class, ex);
    }

    // --- MultipleValidationException ---

    @Test
    @DisplayName("MultipleValidationException wraps a BindingResult and has a fixed message")
    void multipleValidationException_withBindingResult_storesIt() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "orderRequest");
        bindingResult.addError(new FieldError("orderRequest", "phone", "Phone is required"));

        MultipleValidationException ex = new MultipleValidationException(bindingResult);

        assertEquals("Validation failed", ex.getMessage());
        assertSame(bindingResult, ex.getBindingResult());
        assertTrue(ex.getBindingResult().hasErrors());
        assertEquals(1, ex.getBindingResult().getFieldErrorCount());
    }

    @Test
    @DisplayName("MultipleValidationException with no field errors still stores BindingResult")
    void multipleValidationException_noErrors_storesBindingResult() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "userRequest");

        MultipleValidationException ex = new MultipleValidationException(bindingResult);

        assertEquals("Validation failed", ex.getMessage());
        assertFalse(ex.getBindingResult().hasErrors());
    }
}
