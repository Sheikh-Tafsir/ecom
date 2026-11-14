package com.example.ecom.cms.banner.validator;

import com.example.ecom.cms.banner.dto.BannerRequest;
import com.example.ecom.cms.banner.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.UUID;

@Component
@RefreshScope
@RequiredArgsConstructor
public class BannerValidator {

    @Value("${app.cms.max-total-banners:20}")
    private int maxTotalBanners;

    @Value("${app.cms.max-active-banners:5}")
    private int maxActiveBanners;

    private final BannerRepository bannerRepository;

    public void validateCreate(BannerRequest request, Errors errors) {
        if (bannerRepository.count() >= maxTotalBanners) {
            errors.reject("error.limit.exceeded", new Object[]{"Banners", maxTotalBanners}, "Total Banners limit exceeded, cannot add more than " + maxTotalBanners);
        }

        if (request.isActive() && bannerRepository.countByActiveTrue() >= maxActiveBanners) {
            errors.rejectValue("active", "error.limit.exceeded", new Object[]{"Active Banners", maxActiveBanners}, "Total Active Banners limit exceeded, cannot add more than " + maxActiveBanners);
        }

        if (request.getDisplayOrder() != 0 && bannerRepository.existsByDisplayOrder(request.getDisplayOrder())) {
            errors.rejectValue("displayOrder", "error.field.duplicate", new Object[]{"Display Order"}, "Display order already exists");
        }
    }

    public void validateUpdate(UUID id, BannerRequest request, Errors errors) {
        if (request.isActive()) {
            bannerRepository.findById(id).ifPresent(banner -> {
                if (!banner.isActive() && bannerRepository.countByActiveTrue() >= maxActiveBanners) {
                    errors.rejectValue("active", "error.limit.exceeded", new Object[]{"Active Banners", maxActiveBanners}, "Active Banners limit exceeded");
                }
            });
        }

        if (request.getDisplayOrder() != 0 && bannerRepository.existsByDisplayOrderAndIdNot(request.getDisplayOrder(), id)) {
            errors.rejectValue("displayOrder", "error.field.duplicate", new Object[]{"Display Order"}, "Display order already exists");
        }
    }
}
