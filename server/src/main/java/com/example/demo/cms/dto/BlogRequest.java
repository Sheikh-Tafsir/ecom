package com.example.demo.cms.dto;

import com.example.demo.common.enums.BlogPostStatus;
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
}
