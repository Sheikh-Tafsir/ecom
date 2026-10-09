package com.example.ecom.cms.blogs.dto;

import com.example.ecom.common.enums.BlogPostStatus;
import com.example.ecom.common.model.Blogs;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class BlogResponse {

    private UUID id;

    private String title;

    private String content;

    private String author;

    private String imageUrl;

    private BlogPostStatus status;

    private Instant publishedAt;

    private Instant createdAt;

    public BlogResponse(Blogs post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.author = post.getAuthor();
        this.imageUrl = post.getImageUrl();
        this.status = post.getStatus();
        this.publishedAt = post.getPublishedAt();
        this.createdAt = post.getCreatedAt();
    }
}
