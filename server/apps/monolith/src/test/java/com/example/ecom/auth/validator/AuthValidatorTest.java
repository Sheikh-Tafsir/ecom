package com.example.ecom.auth.validator;

import com.example.ecom.auth.dto.SignupRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthValidatorTest {

    @InjectMocks
    private AuthValidator authValidator;

    private Errors errors;

    @BeforeEach
    void setUp() {
        errors = new MapBindingResult(new HashMap<>(), "request");
    }

    // ========== validateSignup ==========

    @Test
    @DisplayName("Should not reject when password and confirmPassword match")
    void validateSignup_matchingPasswords_noErrors() {
        SignupRequest request = new SignupRequest("Jane Doe", "jane@example.com",
                "Str0ngP@ss", "Str0ngP@ss");

        authValidator.validateSignup(request, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("Should reject when password and confirmPassword do not match")
    void validateSignup_mismatchedPasswords_rejectsWithError() {
        SignupRequest request = new SignupRequest("Jane Doe", "jane@example.com",
                "Str0ngP@ss", "DifferentP@ss");

        authValidator.validateSignup(request, errors);

        assertTrue(errors.hasErrors());
        assertNotNull(errors.getFieldError("newPassword"));
        assertEquals("error.password.confirm.password.not.match",
                errors.getFieldError("newPassword").getCode());
    }

    // ========== validatePasswords ==========

    @Test
    @DisplayName("Should not reject when both passwords are null")
    void validatePasswords_bothNull_noErrors() {
        authValidator.validatePasswords(null, null, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("Should not reject when password is null and confirmPassword has text")
    void validatePasswords_passwordNull_noErrors() {
        authValidator.validatePasswords(null, "SomeP@ssword", errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("Should not reject when confirmPassword is null and password has text")
    void validatePasswords_confirmPasswordNull_noErrors() {
        authValidator.validatePasswords("SomeP@ssword", null, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("Should not reject when both passwords are empty strings")
    void validatePasswords_bothEmpty_noErrors() {
        authValidator.validatePasswords("", "", errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("Should not reject when both passwords are blank spaces")
    void validatePasswords_bothBlank_noErrors() {
        authValidator.validatePasswords("   ", "   ", errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("Should reject when both have text but do not match")
    void validatePasswords_mismatchedPasswords_rejectsField() {
        authValidator.validatePasswords("Password1", "Password2", errors);

        assertTrue(errors.hasErrors());
        assertNotNull(errors.getFieldError("newPassword"));
    }

    @Test
    @DisplayName("Should not reject when both have text and match exactly")
    void validatePasswords_matchingPasswords_noErrors() {
        authValidator.validatePasswords("ExactM@tch1", "ExactM@tch1", errors);

        assertFalse(errors.hasErrors());
    }
}
