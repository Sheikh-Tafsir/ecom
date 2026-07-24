package com.example.demo.cms.validator;

import com.example.demo.cms.dto.BannerRequest;
import com.example.demo.cms.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

@Component
@RequiredArgsConstructor
public class BannerValidator {

    public static final int MAX_TOTAL_BANNERS = 20;
    public static final int MAX_ACTIVE_BANNERS = 5;

    private final BannerRepository bannerRepository;

    public void validateCreate(BannerRequest request, Errors errors) {
        if (bannerRepository.count() >= MAX_TOTAL_BANNERS) {
            errors.reject("error.limit.exceeded", new Object[]{"Banners", MAX_TOTAL_BANNERS}, "Total Banners limit exceeded, cannot add more than " + MAX_TOTAL_BANNERS);
        }

        if (request.isActive() && bannerRepository.countByActiveTrue() >= MAX_ACTIVE_BANNERS) {
            errors.rejectValue("active", "error.limit.exceeded", new Object[]{"Active Banners", MAX_ACTIVE_BANNERS}, "Total Active Banners limit exceeded, cannot add more than " + MAX_ACTIVE_BANNERS);
        }

        if (request.getDisplayOrder() != 0 && bannerRepository.existsByDisplayOrder(request.getDisplayOrder())) {
            errors.rejectValue("displayOrder", "error.field.duplicate", new Object[]{"Display Order"}, "Display order already exists");
        }
    }

    public void validateUpdate(Long id, BannerRequest request, Errors errors) {
        if (request.isActive()) {
            bannerRepository.findById(id).ifPresent(banner -> {
                if (!banner.isActive() && bannerRepository.countByActiveTrue() >= MAX_ACTIVE_BANNERS) {
                    errors.rejectValue("active", "error.limit.exceeded", new Object[]{"Active Banners", MAX_ACTIVE_BANNERS}, "Active Banners limit exceeded");
                }
            });
        }

        if (request.getDisplayOrder() != 0 && bannerRepository.existsByDisplayOrderAndIdNot(request.getDisplayOrder(), id)) {
            errors.rejectValue("displayOrder", "error.field.duplicate", new Object[]{"Display Order"}, "Display order already exists");
        }
    }
}
