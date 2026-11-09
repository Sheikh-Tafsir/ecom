package com.example.ecom.product.review.service;

import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.common.model.Product;
import com.example.ecom.common.model.Review;
import com.example.ecom.common.model.User;
import com.example.ecom.common.service.MessageService;
import com.example.ecom.product.product.service.ProductService;
import com.example.ecom.product.review.dto.CreateReviewRequest;
import com.example.ecom.product.review.dto.ReviewResponse;
import com.example.ecom.product.review.dto.UpdateReviewRequest;
import com.example.ecom.product.review.repository.ReviewRepository;
import com.example.ecom.user.user.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.ecom.common.utils.CacheConstants.CACHE_PRODUCTS;
import static com.example.ecom.common.utils.CacheConstants.CACHE_PRODUCTS_EDIT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ProductService productService;

    @Mock
    private MessageService messageService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserService userService;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private ReviewService reviewService;

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_ID_OTHER = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID REVIEW_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private Product product;
    private User user;
    private Review review;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(PRODUCT_ID);
        product.setName("Gaming Laptop");
        product.setPrice(new BigDecimal("1299.99"));
        product.setRating(new BigDecimal("4.00"));
        product.setReviewCount(10);

        user = new User();
        user.setId(USER_ID);
        user.setName("John Doe");
        user.setEmail("john.doe@example.com");

        userDetails = new CustomUserDetails(user);

        review = new Review();
        review.setId(REVIEW_ID);
        review.setUser(user);
        review.setProduct(product);
        review.setRating(4);
        review.setComment("Great laptop for gaming");
    }

    @Test
    @DisplayName("findAllByProduct should return paginated reviews for a product")
    void findAllByProduct_existingProduct_returnsPaginatedReviews() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> reviewPage = new PageImpl<>(List.of(review), pageable, 1);
        when(reviewRepository.findAllByProduct_Id(eq(PRODUCT_ID), any(Pageable.class))).thenReturn(reviewPage);

        Page<ReviewResponse> result = reviewService.findAllByProduct(PRODUCT_ID, pageable);

        assertEquals(1, result.getTotalElements());
        verify(reviewRepository).findAllByProduct_Id(eq(PRODUCT_ID), any(Pageable.class));
    }

    @Test
    @DisplayName("findAllByProduct should return empty page when no reviews exist")
    void findAllByProduct_noReviews_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(reviewRepository.findAllByProduct_Id(eq(PRODUCT_ID), any(Pageable.class))).thenReturn(emptyPage);

        Page<ReviewResponse> result = reviewService.findAllByProduct(PRODUCT_ID, pageable);

        assertEquals(0, result.getTotalElements());
        verify(reviewRepository).findAllByProduct_Id(eq(PRODUCT_ID), any(Pageable.class));
    }

    @Test
    @DisplayName("create should save review and update product rating when user has not reviewed")
    void create_firstReview_savesAndUpdatesRating() {
        product.setReviewCount(0);
        product.setRating(BigDecimal.ZERO);

        CreateReviewRequest request = new CreateReviewRequest(5, "Amazing product!");

        when(reviewRepository.existsByUser_IdAndProduct_Id(USER_ID, PRODUCT_ID)).thenReturn(false);
        when(productService.findByIdHelper(PRODUCT_ID)).thenReturn(product);
        when(userService.findByIdHelper(USER_ID)).thenReturn(user);
        when(cacheManager.getCache(CACHE_PRODUCTS)).thenReturn(mock(Cache.class));
        when(cacheManager.getCache(CACHE_PRODUCTS_EDIT)).thenReturn(mock(Cache.class));

        reviewService.create(PRODUCT_ID, request, userDetails);

        assertEquals(1, product.getReviewCount());
        assertEquals(new BigDecimal("5.00"), product.getRating());
        verify(reviewRepository).save(any(Review.class));
        verify(reviewRepository).existsByUser_IdAndProduct_Id(USER_ID, PRODUCT_ID);
    }

    @Test
    @DisplayName("create should calculate correct running average when adding to existing reviews")
    void create_additionalReview_updatesAverageCorrectly() {
        product.setReviewCount(4);
        product.setRating(new BigDecimal("4.00"));

        CreateReviewRequest request = new CreateReviewRequest(2, "Not great");

        when(reviewRepository.existsByUser_IdAndProduct_Id(USER_ID, PRODUCT_ID)).thenReturn(false);
        when(productService.findByIdHelper(PRODUCT_ID)).thenReturn(product);
        when(userService.findByIdHelper(USER_ID)).thenReturn(user);
        when(cacheManager.getCache(CACHE_PRODUCTS)).thenReturn(mock(Cache.class));
        when(cacheManager.getCache(CACHE_PRODUCTS_EDIT)).thenReturn(mock(Cache.class));

        reviewService.create(PRODUCT_ID, request, userDetails);

        assertEquals(5, product.getReviewCount());
        assertEquals(new BigDecimal("3.60"), product.getRating());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("create should throw IllegalArgumentException when user already reviewed the product")
    void create_alreadyReviewed_throwsIllegalArgumentException() {
        CreateReviewRequest request = new CreateReviewRequest(5, "Great!");

        when(reviewRepository.existsByUser_IdAndProduct_Id(USER_ID, PRODUCT_ID)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> reviewService.create(PRODUCT_ID, request, userDetails));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should update rating and recalculate product average when rating changes")
    void update_ratingChanged_updatesRatingAndRecalculates() {
        product.setReviewCount(5);
        product.setRating(new BigDecimal("4.00"));
        review.setRating(3);

        UpdateReviewRequest request = new UpdateReviewRequest(5, "Updated comment");

        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(cacheManager.getCache(CACHE_PRODUCTS)).thenReturn(mock(Cache.class));
        when(cacheManager.getCache(CACHE_PRODUCTS_EDIT)).thenReturn(mock(Cache.class));

        reviewService.update(REVIEW_ID, request, userDetails);

        assertEquals(5, review.getRating());
        assertEquals("Updated comment", review.getComment());
        verify(reviewRepository).save(review);
    }

    @Test
    @DisplayName("update should only update comment when rating is same")
    void update_sameRating_onlyUpdatesComment() {
        UpdateReviewRequest request = new UpdateReviewRequest(4, "Updated comment only");

        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(cacheManager.getCache(CACHE_PRODUCTS)).thenReturn(mock(Cache.class));
        when(cacheManager.getCache(CACHE_PRODUCTS_EDIT)).thenReturn(mock(Cache.class));

        reviewService.update(REVIEW_ID, request, userDetails);

        assertEquals("Updated comment only", review.getComment());
        assertEquals(new BigDecimal("4.00"), product.getRating());
        verify(reviewRepository).save(review);
    }

    @Test
    @DisplayName("update should throw AccessDeniedException when user does not own the review")
    void update_notOwner_throwsAccessDeniedException() {
        User otherUser = new User();
        otherUser.setId(USER_ID_OTHER);
        otherUser.setName("Other User");
        otherUser.setEmail("other@example.com");
        CustomUserDetails otherDetails = new CustomUserDetails(otherUser);

        UpdateReviewRequest request = new UpdateReviewRequest(5, "Hacked");

        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        assertThrows(AccessDeniedException.class, () -> reviewService.update(REVIEW_ID, request, otherDetails));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should throw EntityNotFoundException when review not found")
    void update_reviewNotFound_throwsEntityNotFoundException() {
        UpdateReviewRequest request = new UpdateReviewRequest(5, "Updated");

        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());
        when(messageService.get(eq("error.entity.not.found"), eq("Review"), eq(REVIEW_ID)))
                .thenReturn("Review not found");

        assertThrows(EntityNotFoundException.class, () -> reviewService.update(REVIEW_ID, request, userDetails));
    }

    @Test
    @DisplayName("delete should remove review and recalculate product rating")
    void delete_ownerDeletes_removesAndRecalculatesRating() {
        product.setReviewCount(5);
        product.setRating(new BigDecimal("4.00"));
        review.setRating(3);

        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(cacheManager.getCache(CACHE_PRODUCTS)).thenReturn(mock(Cache.class));
        when(cacheManager.getCache(CACHE_PRODUCTS_EDIT)).thenReturn(mock(Cache.class));

        reviewService.delete(REVIEW_ID, userDetails);

        assertEquals(4, product.getReviewCount());
        verify(reviewRepository).delete(review);
    }

    @Test
    @DisplayName("delete should set rating to zero when deleting the last review")
    void delete_lastReview_setsRatingToZero() {
        product.setReviewCount(1);
        product.setRating(new BigDecimal("5.00"));
        review.setRating(5);

        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(cacheManager.getCache(CACHE_PRODUCTS)).thenReturn(mock(Cache.class));
        when(cacheManager.getCache(CACHE_PRODUCTS_EDIT)).thenReturn(mock(Cache.class));

        reviewService.delete(REVIEW_ID, userDetails);

        assertEquals(0, product.getReviewCount());
        assertEquals(BigDecimal.ZERO, product.getRating());
        verify(reviewRepository).delete(review);
    }

    @Test
    @DisplayName("delete should throw AccessDeniedException when non-owner non-admin tries to delete")
    void delete_notOwnerNotAdmin_throwsAccessDeniedException() {
        User otherUser = new User();
        otherUser.setId(USER_ID_OTHER);
        otherUser.setName("Other User");
        otherUser.setEmail("other@example.com");
        CustomUserDetails otherDetails = new CustomUserDetails(otherUser);

        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        assertThrows(AccessDeniedException.class, () -> reviewService.delete(REVIEW_ID, otherDetails));
        verify(reviewRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete should throw EntityNotFoundException when review not found")
    void delete_reviewNotFound_throwsEntityNotFoundException() {
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());
        when(messageService.get(eq("error.entity.not.found"), eq("Review"), eq(REVIEW_ID)))
                .thenReturn("Review not found");

        assertThrows(EntityNotFoundException.class, () -> reviewService.delete(REVIEW_ID, userDetails));
    }

    @Test
    @DisplayName("update should handle null comment in request by not modifying the comment")
    void update_nullComment_doesNotModifyComment() {
        review.setComment("Original comment");
        UpdateReviewRequest request = new UpdateReviewRequest(4, null);

        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(cacheManager.getCache(CACHE_PRODUCTS)).thenReturn(mock(Cache.class));
        when(cacheManager.getCache(CACHE_PRODUCTS_EDIT)).thenReturn(mock(Cache.class));

        reviewService.update(REVIEW_ID, request, userDetails);

        assertEquals("Original comment", review.getComment());
        verify(reviewRepository).save(review);
    }

    @Test
    @DisplayName("create should evict product caches after saving review")
    void create_afterSave_evictsProductCaches() {
        product.setReviewCount(0);
        product.setRating(BigDecimal.ZERO);

        Cache productCache = mock(Cache.class);
        Cache productEditCache = mock(Cache.class);

        CreateReviewRequest request = new CreateReviewRequest(4, "Good");

        when(reviewRepository.existsByUser_IdAndProduct_Id(USER_ID, PRODUCT_ID)).thenReturn(false);
        when(productService.findByIdHelper(PRODUCT_ID)).thenReturn(product);
        when(userService.findByIdHelper(USER_ID)).thenReturn(user);
        when(cacheManager.getCache(CACHE_PRODUCTS)).thenReturn(productCache);
        when(cacheManager.getCache(CACHE_PRODUCTS_EDIT)).thenReturn(productEditCache);

        reviewService.create(PRODUCT_ID, request, userDetails);

        verify(productCache).evict(PRODUCT_ID);
        verify(productEditCache).evict(PRODUCT_ID);
    }
}
