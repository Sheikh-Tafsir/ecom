package com.example.demo.review.dto;

import com.example.demo.common.model.Review;
import lombok.Data;

@Data
public class ReviewResponse {

    private Long id;

    private Integer rating;

    private String comment;

    public ReviewResponse(Review review) {
        id = review.getId();
        rating = review.getRating();
        comment = review.getComment();
    }
}
