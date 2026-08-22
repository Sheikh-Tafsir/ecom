package com.example.ecom.product.review.validator;

import com.example.ecom.product.review.dto.UpdateReviewRequest;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

@Component
public class ReviewValidator {

    public void validateUpdate(UpdateReviewRequest request, Errors errors) {
        if (request.rating() == null && request.comment() == null) {
            errors.reject("", "Rating and Review both cannot be empty");
        }
    }
}
