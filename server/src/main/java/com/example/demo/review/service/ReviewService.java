package com.example.demo.review.service;

import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.helper.CommonHelper;
import com.example.demo.common.model.Product;
import com.example.demo.common.model.Review;
import com.example.demo.common.service.MessageService;
import com.example.demo.product.service.ProductService;
import com.example.demo.review.dto.CreateReviewRequest;
import com.example.demo.review.dto.UpdateReviewRequest;
import com.example.demo.review.repository.ReviewRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.demo.common.utils.Utils.getValidPageable;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ModelMapper modelMapper;

    private final ProductService productService;

    private final MessageService messageService;

    private final ReviewRepository reviewRepository;

    private final CommonHelper commonHelper;

    public Page<Review> findAllByProductAndUser(Long productId, Pageable pageable, CustomUserDetails userDetails) {
        return reviewRepository.findAllByProduct_IdAndUser_Id(productId, userDetails.getId(), getValidPageable(pageable));
    }

    @Transactional
    public void create(Long id, CreateReviewRequest request, CustomUserDetails userDetails) {
        Product product = productService.findByIdHelper(id);

        Review review = modelMapper.map(request, Review.class);
        review.setUser(userDetails.user());
        review.setProduct(product);

        reviewRepository.save(review);
    }

    @Transactional
    public void update(Long id, UpdateReviewRequest request, CustomUserDetails userDetails) {
        Review review = findByIdHelper(id);
        commonHelper.checkOwner(review.getUser().getId(), userDetails);

        if (request.rating() != null) {
            review.setRating(request.rating());
        }

        if (request.comment() != null) {
            review.setComment(request.comment());
        }

        reviewRepository.save(review);
    }

    @Transactional
    public void delete(Long id,  CustomUserDetails userDetails) {
        Review review = findByIdHelper(id);
        commonHelper.checkOwner(review.getUser().getId(), userDetails);

        reviewRepository.delete(review);
    }

    private Review findByIdHelper(Long id) {
        return reviewRepository.findById(id).
                orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "Review", id)));
    }
}
