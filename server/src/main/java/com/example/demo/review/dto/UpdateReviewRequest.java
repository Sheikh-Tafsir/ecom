package com.example.demo.review.dto;

public record UpdateReviewRequest(

        Integer rating,

        String comment
) {
}
