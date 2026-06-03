package com.example.demo.review.dto;

import lombok.NonNull;

public record CreateReviewRequest(

        @NonNull
        Long productId,

        @NonNull
        Integer rating,

        String comment
) {
}
