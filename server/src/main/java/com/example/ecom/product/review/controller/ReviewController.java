package com.example.ecom.product.review.controller;

import com.example.ecom.common.dto.ApiResponse;
import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.common.service.MessageService;
import com.example.ecom.common.utils.ResponseUtils;
import com.example.ecom.product.review.dto.UpdateReviewRequest;
import com.example.ecom.product.review.service.ReviewService;
import com.example.ecom.product.review.validator.ReviewValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import static com.example.ecom.common.utils.Utils.checkErrors;

@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewValidator reviewValidator;

    private final ReviewService reviewService;

    private final MessageService messageService;

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable Long id,
                                                    @Valid @RequestBody UpdateReviewRequest request,
                                                    BindingResult bindingResult,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        reviewValidator.validateUpdate(request, bindingResult);
        checkErrors(bindingResult);

        reviewService.update(id, request, userDetails);

        return ResponseUtils.created(messageService.get("entity.updating", "Review"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {

        reviewService.delete(id, userDetails);
        return ResponseUtils.ok(messageService.get("entity.deleting", "Review"));
    }
}
