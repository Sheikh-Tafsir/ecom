package com.example.ecom.cms.banner.dto;

import com.example.ecom.common.model.Banner;

import java.util.UUID;

public record BannerResponse(
        UUID id,
        String title,
        String subtitle,
        String imageUrl,
        String linkUrl,
        int displayOrder,
        boolean active
) {
    public BannerResponse(Banner banner) {
        this(
                banner.getId(),
                banner.getTitle(),
                banner.getSubtitle(),
                banner.getImageUrl(),
                banner.getLinkUrl(),
                banner.getDisplayOrder(),
                banner.isActive()
        );
    }
}
