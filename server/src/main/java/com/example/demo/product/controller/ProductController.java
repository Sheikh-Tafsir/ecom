package com.example.demo.product.controller;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.service.MessageService;
import com.example.demo.common.utils.ResponseUtils;
import com.example.demo.product.dto.*;
import com.example.demo.product.service.ProductService;
import com.example.demo.product.validator.ProductValidator;
import com.example.demo.review.dto.CreateReviewRequest;
import com.example.demo.review.dto.ReviewResponse;
import com.example.demo.review.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PastOrPresent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;

import static com.example.demo.common.utils.Utils.checkErrors;

@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductValidator productValidator;

    private final ProductService productService;

    private final ReviewService reviewService;

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductListResponse>>> findAll(Pageable pageable,
                                                                          @RequestParam(required = false) String name,
                                                                          @RequestParam(required = false) String category,
                                                                          @RequestParam(required = false) @PastOrPresent LocalDate fromDate,
                                                                          @RequestParam(required = false) @PastOrPresent LocalDate toDate,
                                                                          @AuthenticationPrincipal CustomUserDetails userDetails) {

        Page<ProductListResponse> products = productService.findAll(pageable, name, category, fromDate, toDate, userDetails);
        return ResponseUtils.ok(products, messageService.get("successfully.found", "Product List"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ProductListResponse>>> search(@RequestParam String name) {
        Page<ProductListResponse> products = productService.search(name);
        return ResponseUtils.ok(products, messageService.get("successfully.found", "Product Search"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> findById(@PathVariable Long id) {
        ProductResponse product = productService.findById(id);
        return ResponseUtils.ok(product, messageService.get("successfully.found", "Product"));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> create(@Valid @ModelAttribute CreateProductRequest productRequest,
                                                                   BindingResult bindingResult) throws IOException {

        productValidator.validateCreate(productRequest, bindingResult);
        checkErrors(bindingResult);

        long id = productService.create(productRequest);
        return ResponseUtils.created(id, messageService.get("entity.creating", "Product"));
    }

    @GetMapping("/{id}/edit")
    public ResponseEntity<ApiResponse<ProductEditResponse>> findEditById(@PathVariable Long id) {
        ProductEditResponse product = productService.findEditById(id);
        return ResponseUtils.ok(product, messageService.get("successfully.found", "Product"));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductEditResponse>> update(@PathVariable Long id,
                                                                   @Valid @ModelAttribute UpdateProductRequest productRequest,
                                                                   BindingResult bindingResult) throws IOException {

        productValidator.validateUpdate(productRequest, bindingResult);
        checkErrors(bindingResult);

        productService.update(id, productRequest);
        return ResponseUtils.ok(messageService.get("successfully.updated", "Product"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseUtils.ok(messageService.get("successfully.deleted", "Product"));
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getReviews(@PathVariable Long id,
                                                                        Pageable pageable) {

        Page<ReviewResponse> reviews = reviewService.findAllByProduct(id, pageable);
        return ResponseUtils.ok(reviews, messageService.get("successfully.found", "Reviews"));
    }
    @PostMapping("/{id}/review")
    public ResponseEntity<ApiResponse<Void>> addReview(@PathVariable Long id,
                                                       @Valid @RequestBody CreateReviewRequest request,
                                                       @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("request: {}", request);
        reviewService.create(id, request, userDetails);
        return ResponseUtils.created(messageService.get("entity.creating", "Review"));
    }
}
