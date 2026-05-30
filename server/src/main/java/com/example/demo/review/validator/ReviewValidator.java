package com.example.demo.review.validator;

import com.example.demo.review.dto.CreateReviewRequest;
import com.example.demo.review.dto.UpdateReviewRequest;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

@Component
public class ReviewValidator {

    public void validateCreate(CreateReviewRequest request, Errors errors) {
        validate(request.rating(), request.comment(), errors);
    }

    public void validateUpdate(UpdateReviewRequest request, Errors errors) {
        validate(request.rating(), request.comment(), errors);
    }

    private void validate(Integer rating, String comment, Errors errors) {
        if (rating == null && comment == null) {
            errors.reject("", "Rating and Review both cannot be empty");
        }
    }
}
