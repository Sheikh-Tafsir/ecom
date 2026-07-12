package com.example.demo.review.service;

import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.model.Product;
import com.example.demo.common.model.Review;
import com.example.demo.common.model.User;
import com.example.demo.common.service.MessageService;
import com.example.demo.product.service.ProductService;
import com.example.demo.review.dto.CreateReviewRequest;
import com.example.demo.review.dto.ReviewResponse;
import com.example.demo.review.dto.UpdateReviewRequest;
import com.example.demo.review.repository.ReviewRepository;
import com.example.demo.user.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static com.example.demo.common.enums.Permission.ADMIN_ACCESS;
import static com.example.demo.common.enums.Permission.SUPER_ADMIN_ACCESS;
import static com.example.demo.common.utils.SecurityUtil.*;
import static com.example.demo.common.utils.Utils.getValidPageable;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ProductService productService;

    private final MessageService messageService;

    private final ReviewRepository reviewRepository;

    private final UserService userService;

    public Page<ReviewResponse> findAllByProduct(Long productId, Pageable pageable) {
        return reviewRepository.findAllByProduct_Id(productId, getValidPageable(pageable)).map(ReviewResponse::new);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "productEdit", key = "#id")
    })
    public void create(Long id, CreateReviewRequest request, CustomUserDetails userDetails) {
        Product product = productService.findByIdHelper(id);

        long oldReviewCount = product.getReviewCount();
        BigDecimal oldAvgRating = product.getRating();

        BigDecimal newRating = BigDecimal.valueOf(request.rating());

        long newReviewCount = oldReviewCount + 1;

        BigDecimal newAvgRating = oldAvgRating
                .multiply(BigDecimal.valueOf(oldReviewCount))
                .add(newRating)
                .divide(BigDecimal.valueOf(newReviewCount), 2, RoundingMode.HALF_UP);

        product.setReviewCount(newReviewCount);
        product.setRating(newAvgRating);

        User user = userService.findByIdHelper(userDetails.getId());

        Review review =new Review();
        review.setRating(request.rating());
        review.setComment(request.comment());
        review.setUser(user);
        review.setProduct(product);

        reviewRepository.save(review);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "product", key = "#review.product.id"),
            @CacheEvict(value = "productEdit", key = "#review.product.id")
    })
    public void update(Long id, UpdateReviewRequest request, CustomUserDetails userDetails) {
        Review review = findByIdHelper(id);

        if (!isOwner(review.getUser().getId(), userDetails)) {
            throwAccessException(review.getUser().getId(), userDetails.getId(), "Review", review.getId());
        }

        if (request.rating() != null && !request.rating().equals(review.getRating())) {
            Product product = review.getProduct();
            BigDecimal oldRating = BigDecimal.valueOf(review.getRating());
            BigDecimal newRating = BigDecimal.valueOf(request.rating());

            BigDecimal newAvgRating = product.getRating()
                    .multiply(BigDecimal.valueOf(product.getReviewCount()))
                    .subtract(oldRating)
                    .add(newRating)
                    .divide(BigDecimal.valueOf(product.getReviewCount()), 2, RoundingMode.HALF_UP);

            product.setRating(newAvgRating);
            review.setRating(request.rating());
        }

        if (request.comment() != null) {
            review.setComment(request.comment());
        }

        reviewRepository.save(review);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "product", key = "#review.product.id"),
            @CacheEvict(value = "productEdit", key = "#review.product.id")
    })
    public void delete(Long id, CustomUserDetails userDetails) {
        Review review = findByIdHelper(id);
        if (!isOwner(review.getUser().getId(), userDetails)
                && !hasPermission(List.of(SUPER_ADMIN_ACCESS.getValue(), ADMIN_ACCESS.getValue()), userDetails)) {
            throwAccessException(review.getUser().getId(), userDetails.getId(), "Review", review.getId());
        }

        Product product = review.getProduct();
        long oldReviewCount = product.getReviewCount();
        BigDecimal oldAvgRating = product.getRating();

        long newReviewCount = oldReviewCount - 1;

        if (newReviewCount == 0) {
            product.setRating(BigDecimal.ZERO);
        } else {
            BigDecimal newAvgRating = oldAvgRating
                    .multiply(BigDecimal.valueOf(oldReviewCount))
                    .subtract(BigDecimal.valueOf(review.getRating()))
                    .divide(BigDecimal.valueOf(newReviewCount), 2, RoundingMode.HALF_UP);
            product.setRating(newAvgRating);
        }

        product.setReviewCount(newReviewCount);

        reviewRepository.delete(review);
    }

    // -- helpers --
    private Review findByIdHelper(Long id) {
        return reviewRepository.findById(id).
                orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "Review", id)));
    }
}
