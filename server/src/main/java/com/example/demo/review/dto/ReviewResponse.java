package com.example.demo.review.dto;

import com.example.demo.common.model.Review;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReviewResponse {

    private Long id;

    private Integer rating;

    private String comment;

    private String userName;

    private String userImage;

    private LocalDateTime createdAt;

    public ReviewResponse(Review review) {
        id = review.getId();
        rating = review.getRating();
        comment = review.getComment();
        userName = review.getUser().getName();
        userImage = review.getUser().getImage();
        createdAt = review.getCreatedAt();
    }
}
