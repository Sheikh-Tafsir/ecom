package com.example.ecom.cms.banner.dto;

import com.example.ecom.common.model.Banner;
import lombok.Data;

@Data
public class BannerResponse {

    private Long id;

    private String title;

    private String subtitle;

    private String imageUrl;

    private String linkUrl;

    private int displayOrder;

    private boolean active;

    public BannerResponse(Banner banner) {
        this.id = banner.getId();
        this.title = banner.getTitle();
        this.subtitle = banner.getSubtitle();
        this.imageUrl = banner.getImageUrl();
        this.linkUrl = banner.getLinkUrl();
        this.displayOrder = banner.getDisplayOrder();
        this.active = banner.isActive();
    }
}
