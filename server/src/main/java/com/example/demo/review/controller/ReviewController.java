package com.example.demo.review.controller;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.service.MessageService;
import com.example.demo.common.utils.ResponseUtils;
import com.example.demo.review.dto.UpdateReviewRequest;
import com.example.demo.review.service.ReviewService;
import com.example.demo.review.validator.ReviewValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import static com.example.demo.common.utils.Utils.checkErrors;

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
