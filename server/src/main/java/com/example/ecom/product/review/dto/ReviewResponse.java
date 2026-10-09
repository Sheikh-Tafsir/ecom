package com.example.ecom.product.review.dto;

import com.example.ecom.common.model.Review;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private UUID id;

    private Integer rating;

    private String comment;

    private String userName;

    private String userImage;

    private Instant createdAt;

    public ReviewResponse(Review review) {
        id = review.getId();
        rating = review.getRating();
        comment = review.getComment();
        if (review.getUser() != null) {
            userName = review.getUser().getName();
            userImage = review.getUser().getImage();
        }
        createdAt = review.getCreatedAt();
    }

}
