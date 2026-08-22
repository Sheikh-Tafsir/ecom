package com.example.ecom.cms.blogs.service;

import com.example.ecom.cms.blogs.dto.BlogRequest;
import com.example.ecom.cms.blogs.dto.BlogResponse;
import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.common.enums.BlogPostStatus;
import com.example.ecom.common.model.Blogs;
import com.example.ecom.cms.blogs.repository.BlogRepository;
import com.example.ecom.common.service.MessageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.ecom.common.utils.CacheConstants.CACHE_BLOGS;
import static com.example.ecom.common.enums.Permission.ADMIN_ACCESS;
import static com.example.ecom.common.enums.Permission.SUPER_ADMIN_ACCESS;
import static com.example.ecom.common.utils.SecurityUtil.hasPermission;

@Service
@RequiredArgsConstructor
public class BlogService {

    private final BlogRepository blogRepository;

    private final MessageService messageService;

    public Page<BlogResponse> findAllPublished(String search, Pageable pageable) {
        if (search != null && !search.isEmpty()) {
            return blogRepository.findAllByStatusAndTitleContainingIgnoreCaseOrStatusAndContentContainingIgnoreCase(
                    BlogPostStatus.PUBLISHED, search, BlogPostStatus.PUBLISHED, search, pageable)
                    .map(BlogResponse::new);
        }
        return blogRepository.findAllByStatus(BlogPostStatus.PUBLISHED, pageable)
                .map(BlogResponse::new);
    }

    @Cacheable(value = CACHE_BLOGS, key = "#title", condition = "#userDetails == null")
    public BlogResponse findByTitle(String title, CustomUserDetails userDetails) {
        Blogs post = blogRepository.findByTitle(title)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "BlogPost", title)));

        boolean isAdmin = hasPermission(List.of(ADMIN_ACCESS.getValue(), SUPER_ADMIN_ACCESS.getValue()), userDetails);

        if (post.getStatus() == BlogPostStatus.DRAFT && !isAdmin) {
            throw new EntityNotFoundException(messageService.get("error.entity.not.found", "BlogPost", title));
        }

        return new BlogResponse(post);
    }

    @PreAuthorize("hasAnyAuthority(T(com.example.ecom.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    public Page<BlogResponse> findAll(String search, Pageable pageable) {
        if (search != null && !search.isEmpty()) {
            return blogRepository.findAllByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(search, search, pageable)
                    .map(BlogResponse::new);
        }
        return blogRepository.findAll(pageable).map(BlogResponse::new);
    }

    @PreAuthorize("hasAnyAuthority(T(com.example.ecom.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Transactional
    @CacheEvict(value = CACHE_BLOGS, allEntries = true)
    public BlogResponse create(BlogRequest request) {
        Blogs post = new Blogs();
        updatePostFromRequest(post, request);
        return new BlogResponse(blogRepository.save(post));
    }

    @PreAuthorize("hasAnyAuthority(T(com.example.ecom.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Transactional
    @CacheEvict(value = CACHE_BLOGS, allEntries = true)
    public BlogResponse update(Long id, BlogRequest request) {
        Blogs post = findByIdHelper(id);
        updatePostFromRequest(post, request);
        return new BlogResponse(blogRepository.save(post));
    }

    @PreAuthorize("hasAnyAuthority(T(com.example.ecom.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Transactional
    @CacheEvict(value = CACHE_BLOGS, allEntries = true)
    public void delete(Long id) {
        Blogs post = findByIdHelper(id);
        blogRepository.delete(post);
    }

    private Blogs findByIdHelper(Long id) {
        return blogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "BlogPost", id)));
    }

    private void updatePostFromRequest(Blogs post, BlogRequest request) {
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setAuthor(request.getAuthor());
        post.setImageUrl(request.getImageUrl());
        
        if (post.getStatus() != request.getStatus() && request.getStatus() == BlogPostStatus.PUBLISHED) {
            post.setPublishedAt(java.time.Instant.now());
        }

        post.setStatus(request.getStatus());
    }
}
