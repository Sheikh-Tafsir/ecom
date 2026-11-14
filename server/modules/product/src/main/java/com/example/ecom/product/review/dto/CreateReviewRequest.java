package com.example.ecom.product.review.dto;

import jakarta.validation.constraints.Size;
import lombok.NonNull;

public record CreateReviewRequest(

        @NonNull
        Integer rating,

        @Size(max = 1023)
        String comment
) {
}
