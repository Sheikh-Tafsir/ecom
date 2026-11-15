package com.example.ecom.product.review.validator;

import com.example.ecom.product.review.dto.UpdateReviewRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.validation.Errors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReviewValidatorTest {

    @InjectMocks
    private ReviewValidator reviewValidator;

    private Errors errors;

    @BeforeEach
    void setUp() {
        errors = new DirectFieldBindingResult(new Object(), "request");
    }

    @Test
    @DisplayName("validateUpdate should pass when rating is provided")
    void validateUpdate_ratingProvided_noErrors() {
        UpdateReviewRequest request = new UpdateReviewRequest(5, null);

        reviewValidator.validateUpdate(request, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("validateUpdate should pass when both rating and comment are provided")
    void validateUpdate_bothProvided_noErrors() {
        UpdateReviewRequest request = new UpdateReviewRequest(4, "Updated comment");

        reviewValidator.validateUpdate(request, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("validateUpdate should pass when rating is provided with null comment")
    void validateUpdate_ratingWithNullComment_noErrors() {
        UpdateReviewRequest request = new UpdateReviewRequest(3, null);

        reviewValidator.validateUpdate(request, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("validateUpdate should pass with minimum rating value")
    void validateUpdate_minimumRating_noErrors() {
        UpdateReviewRequest request = new UpdateReviewRequest(1, "Minimum rating");

        reviewValidator.validateUpdate(request, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("validateUpdate should pass with maximum rating value")
    void validateUpdate_maximumRating_noErrors() {
        UpdateReviewRequest request = new UpdateReviewRequest(5, "Maximum rating");

        reviewValidator.validateUpdate(request, errors);

        assertFalse(errors.hasErrors());
    }
}
