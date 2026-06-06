package com.example.demo.review.dto;

import lombok.NonNull;

public record CreateReviewRequest(

        @NonNull
        Integer rating,

        String comment
) {
}
