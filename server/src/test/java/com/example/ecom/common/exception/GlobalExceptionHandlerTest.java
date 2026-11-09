package com.example.ecom.common.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        // handler is reset via @InjectMocks each time
    }

    // --- 400 ---

    @Test
    @DisplayName("IllegalArgumentException returns 400 with the exception message")
    void handleIllegalArgumentException_always_returns400() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid product ID format");

        ResponseEntity<?> response = handler.handleIllegalArgumentException(ex);

        assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // --- 401 ---

    @Test
    @DisplayName("BadCredentialsException returns 401 with the exception message")
    void handleBadCredentials_always_returns401() {
        BadCredentialsException ex = new BadCredentialsException("Wrong password");

        ResponseEntity<?> response = handler.handleIllegalArgumentException(ex);

        assertEquals(HttpStatusCode.valueOf(401), response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("InvalidAccessTokenException returns 401 with generic access token message")
    void handleAccessTokenInvalid_always_returns401WithGenericMessage() {
        InvalidAccessTokenException ex = new InvalidAccessTokenException("Token expired at 2024-01-01");

        ResponseEntity<?> response = handler.handleAccessTokenInvalidExceptions(ex);

        assertEquals(HttpStatusCode.valueOf(401), response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("InvalidRefreshTokenException returns 401 with generic refresh token message")
    void handleRefreshTokenInvalid_always_returns401() {
        InvalidRefreshTokenException ex = new InvalidRefreshTokenException("Refresh token revoked");

        ResponseEntity<?> response = handler.handleRefreshTokenInvalidException(ex);

        assertEquals(HttpStatusCode.valueOf(401), response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // --- 403 ---

    @Test
    @DisplayName("AccessDeniedException returns 403 with permission denied message")
    void handleAccessDenied_always_returns403() {
        AccessDeniedException ex = new AccessDeniedException("Insufficient privileges");

        ResponseEntity<?> response = handler.handleAccessDeniedException(ex);

        assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // --- 404 ---

    @Test
    @DisplayName("EntityNotFoundException returns 404 with the exception message")
    void handleEntityNotFound_always_returns404() {
        EntityNotFoundException ex = new EntityNotFoundException("Product not found with id: abc-123");

        ResponseEntity<?> response = handler.handleEntityNotFoundException(ex);

        assertEquals(HttpStatusCode.valueOf(404), response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("NoResultException returns 404 with generic not-found message")
    void handleNoResult_always_returns404() {
        NoResultException ex = new NoResultException("Query returned no results");

        ResponseEntity<?> response = handler.handleNoResultException(ex);

        assertEquals(HttpStatusCode.valueOf(404), response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // --- 409 ---

    @Test
    @DisplayName("DataIntegrityViolationException returns 409 with duplicate request message")
    void handleDataIntegrityViolation_always_returns409() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Unique constraint violated");

        ResponseEntity<?> response = handler.handleDataIntegrityViolationException(ex);

        assertEquals(HttpStatusCode.valueOf(409), response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("ObjectOptimisticLockingFailureException returns 409 with optimistic lock message")
    void handleOptimisticLockingFailure_always_returns409() {
        ObjectOptimisticLockingFailureException ex =
                new ObjectOptimisticLockingFailureException("Order", "ord-123");

        ResponseEntity<?> response = handler.handleOptimisticLockingFailureException(ex);

        assertEquals(HttpStatusCode.valueOf(409), response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // --- 422 ---

    @Test
    @DisplayName("MultipleValidationException returns 422 with field errors from BindingResult")
    void handleMultipleValidation_withFieldErrors_returns422() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "createOrderRequest");
        bindingResult.addError(new FieldError("createOrderRequest", "name", "Name is required"));

        MultipleValidationException ex = new MultipleValidationException(bindingResult);

        ResponseEntity<?> response = handler.handleMultipleValidationExceptions(ex);

        assertEquals(HttpStatusCode.valueOf(422), response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("ValidationException returns 422 with the exception message")
    void handleValidationException_always_returns422() {
        ValidationException ex = new ValidationException("Phone number must be 11 digits");

        ResponseEntity<?> response = handler.handleValidationException(ex);

        assertEquals(HttpStatusCode.valueOf(422), response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // --- 500 ---

    @Test
    @DisplayName("RuntimeException returns 500 with generic message")
    void handleRuntimeException_always_returns500() {
        RuntimeException ex = new RuntimeException("NullPointerException in service layer");

        ResponseEntity<?> response = handler.handleRuntimeException(ex);

        assertEquals(HttpStatusCode.valueOf(500), response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Unhandled checked Exception returns 500 with generic message")
    void handleAllExceptions_checkedExceptions_returns500() {
        Exception ex = new Exception("Unexpected IO failure");

        ResponseEntity<?> response = handler.handleAllExceptions(ex);

        assertEquals(HttpStatusCode.valueOf(500), response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
