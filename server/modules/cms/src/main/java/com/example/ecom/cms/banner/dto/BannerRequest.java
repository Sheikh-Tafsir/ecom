package com.example.ecom.cms.banner.dto;

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

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getLinkUrl() { return linkUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
