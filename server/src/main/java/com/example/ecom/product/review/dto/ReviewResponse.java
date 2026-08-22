package com.example.ecom.product.review.dto;

import com.example.ecom.common.model.Review;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserImage() { return userImage; }
    public void setUserImage(String userImage) { this.userImage = userImage; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
