package com.example.ecom.cms.blogs.dto;

import com.example.ecom.common.enums.BlogPostStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BlogRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    private String author;

    private String imageUrl;

    private BlogPostStatus status;

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
}
