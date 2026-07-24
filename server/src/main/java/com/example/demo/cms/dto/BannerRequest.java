package com.example.demo.cms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BannerRequest {
    @NotBlank
    private String title;

    private String subtitle;

    @NotBlank
    private String imageUrl;

    private String linkUrl;

    private int displayOrder;

    private boolean active = true;
}
