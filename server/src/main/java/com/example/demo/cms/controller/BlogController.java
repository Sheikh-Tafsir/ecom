package com.example.demo.cms.controller;

import com.example.demo.cms.dto.BlogRequest;
import com.example.demo.cms.dto.BlogResponse;
import com.example.demo.cms.service.BlogService;
import com.example.demo.cms.validator.BlogValidator;
import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.service.MessageService;
import com.example.demo.common.utils.ResponseUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import static com.example.demo.common.utils.Utils.checkErrors;

@RestController
@RequestMapping("/blogs")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    private final BlogValidator blogValidator;

    private final MessageService messageService;

    @GetMapping("/published")
    public ResponseEntity<ApiResponse<Page<BlogResponse>>> findAllPublished(@RequestParam(required = false) String search,
                                                                            Pageable pageable) {

        Page<BlogResponse> posts = blogService.findAllPublished(search, pageable);
        return ResponseUtils.ok(posts, messageService.get("successfully.found", "Published Blog List"));
    }

    @GetMapping("/{title}")
    public ResponseEntity<ApiResponse<BlogResponse>> findByTitle(@PathVariable String title,
                                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {

        BlogResponse post = blogService.findByTitle(title, userDetails);
        return ResponseUtils.ok(post, messageService.get("successfully.found", "Blog Post"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BlogResponse>>> findAll(@RequestParam(required = false) String search,
                                                                   Pageable pageable) {

        Page<BlogResponse> posts = blogService.findAll(search, pageable);
        return ResponseUtils.ok(posts, messageService.get("successfully.found", "Blog List"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BlogResponse>> create(@Valid @RequestBody BlogRequest request,
                                                            BindingResult bindingResult) {

        blogValidator.validateCreate(request, bindingResult);
        checkErrors(bindingResult);

        BlogResponse post = blogService.create(request);
        return ResponseUtils.created(post, messageService.get("successfully.created", "Blog Post"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BlogResponse>> update(@PathVariable Long id, @Valid @RequestBody BlogRequest request, BindingResult bindingResult) {
        blogValidator.validateUpdate(id, request, bindingResult);
        checkErrors(bindingResult);

        BlogResponse post = blogService.update(id, request);
        return ResponseUtils.ok(post, messageService.get("successfully.updated", "Blog Post"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        blogService.delete(id);
        return ResponseUtils.ok(messageService.get("successfully.deleted", "Blog Post"));
    }
}
