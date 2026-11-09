package com.example.ecom.product.review.dto;

import com.example.ecom.common.model.Product;
import com.example.ecom.common.model.Review;
import com.example.ecom.common.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReviewDtoTest {

    @Nested
    @DisplayName("CreateReviewRequest Tests")
    class CreateReviewRequestTest {

        @Test
        @DisplayName("Record constructor sets rating and comment")
        void constructor_validArgs_setsFields() {
            CreateReviewRequest request = new CreateReviewRequest(5, "Excellent product!");

            assertEquals(5, request.rating());
            assertEquals("Excellent product!", request.comment());
        }

        @Test
        @DisplayName("Record allows null comment")
        void constructor_nullComment_allowsNull() {
            CreateReviewRequest request = new CreateReviewRequest(3, null);
            assertNull(request.comment());
        }
    }

    @Nested
    @DisplayName("UpdateReviewRequest Tests")
    class UpdateReviewRequestTest {

        @Test
        @DisplayName("Record constructor sets rating and comment")
        void constructor_validArgs_setsFields() {
            UpdateReviewRequest request = new UpdateReviewRequest(4, "Updated comment");

            assertEquals(4, request.rating());
            assertEquals("Updated comment", request.comment());
        }
    }

    @Nested
    @DisplayName("ReviewResponse Tests")
    class ReviewResponseTest {

        @Test
        @DisplayName("Construction from Review entity with user maps all fields")
        void constructor_fromEntityWithUser_setsAllFields() {
            UUID reviewId = UUID.fromString("01927d71-097f-7afa-9ade-071829304152");
            Instant now = Instant.parse("2025-07-10T14:30:00Z");

            User user = new User();
            user.setName("Alex Reviewer");
            user.setImage("https://cdn.example.com/alex.jpg");

            Product product = new Product();
            product.setName("Laptop Stand");

            Review review = new Review();
            review.setId(reviewId);
            review.setUser(user);
            review.setProduct(product);
            review.setRating(4);
            review.setComment("Good quality but a bit pricey");
            review.setCreatedAt(now);

            ReviewResponse response = new ReviewResponse(review);

            assertEquals(reviewId, response.getId());
            assertEquals(4, response.getRating());
            assertEquals("Good quality but a bit pricey", response.getComment());
            assertEquals("Alex Reviewer", response.getUserName());
            assertEquals("https://cdn.example.com/alex.jpg", response.getUserImage());
            assertEquals(now, response.getCreatedAt());
        }

        @Test
        @DisplayName("Construction from Review entity without user sets null user fields")
        void constructor_fromEntityWithoutUser_nullUserFields() {
            Review review = new Review();
            review.setId(UUID.fromString("01927d71-1a8f-7bfa-8adf-182930415263"));
            review.setUser(null);
            review.setRating(2);
            review.setComment("Bad experience");
            review.setCreatedAt(Instant.now());

            ReviewResponse response = new ReviewResponse(review);

            assertNull(response.getUserName());
            assertNull(response.getUserImage());
        }

        @Test
        @DisplayName("No-args constructor creates empty response")
        void noArgsConstructor_createsEmpty() {
            ReviewResponse response = new ReviewResponse();
            assertNull(response.getId());
            assertNull(response.getRating());
            assertNull(response.getComment());
        }
    }
}
