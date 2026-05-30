package com.example.demo.review.dto;

import lombok.NonNull;

public record CreateReviewRequest(

        @NonNull
        Long productId,

        Integer rating,

        String comment
) {
}
