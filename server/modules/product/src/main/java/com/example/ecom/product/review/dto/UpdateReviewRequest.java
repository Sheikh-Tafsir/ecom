package com.example.ecom.product.review.dto;

import jakarta.validation.constraints.Size;
import lombok.NonNull;

public record UpdateReviewRequest(

        @NonNull
        Integer rating,

        @Size(max = 1023)
        String comment
) {
}
