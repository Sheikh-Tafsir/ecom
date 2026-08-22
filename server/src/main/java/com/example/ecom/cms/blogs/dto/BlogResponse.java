package com.example.ecom.cms.blogs.dto;

import com.example.ecom.common.enums.BlogPostStatus;
import com.example.ecom.common.model.Blogs;
import lombok.Data;

import java.time.Instant;

@Data
public class BlogResponse {

    private Long id;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public BlogPostStatus getStatus() { return status; }
    public void setStatus(BlogPostStatus status) { this.status = status; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
