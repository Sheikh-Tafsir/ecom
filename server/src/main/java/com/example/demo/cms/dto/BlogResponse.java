package com.example.demo.cms.dto;

import com.example.demo.common.enums.BlogPostStatus;
import com.example.demo.common.model.Blogs;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlogResponse {

    private Long id;

    private String title;

    private String content;

    private String author;

    private String imageUrl;

    private BlogPostStatus status;

    private LocalDateTime publishedAt;

    private LocalDateTime createdAt;

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
