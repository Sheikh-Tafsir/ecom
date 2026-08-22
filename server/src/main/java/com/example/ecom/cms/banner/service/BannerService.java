package com.example.ecom.cms.banner.service;

import com.example.ecom.cms.banner.dto.BannerRequest;
import com.example.ecom.cms.banner.dto.BannerResponse;
import com.example.ecom.common.model.Banner;
import com.example.ecom.cms.banner.repository.BannerRepository;
import com.example.ecom.common.service.MessageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.example.ecom.common.utils.CacheConstants.CACHE_BANNERS;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;

    private final MessageService messageService;

    @Cacheable(value = CACHE_BANNERS)
    public List<BannerResponse> findAllActive() {
        return bannerRepository.findAllByActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(BannerResponse::new)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyAuthority(T(com.example.ecom.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    public List<BannerResponse> findAll() {
        return bannerRepository.findAll()
                .stream()
                .map(BannerResponse::new)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyAuthority(T(com.example.ecom.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Transactional
    @CacheEvict(value = CACHE_BANNERS, allEntries = true)
    public BannerResponse create(BannerRequest request) {
        Banner banner = new Banner();
        setBannerFromRequest(banner, request);

        if (banner.getDisplayOrder() == 0) {
            banner.setDisplayOrder(bannerRepository.findMaxDisplayOrder().orElse(0) + 1);
        }

        return new BannerResponse(bannerRepository.save(banner));
    }

    @PreAuthorize("hasAnyAuthority(T(com.example.ecom.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Transactional
    @CacheEvict(value = CACHE_BANNERS, allEntries = true)
    public BannerResponse update(Long id, BannerRequest request) {
        Banner banner = findByIdHelper(id);
        setBannerFromRequest(banner, request);
        return new BannerResponse(bannerRepository.save(banner));
    }

    @PreAuthorize("hasAnyAuthority(T(com.example.ecom.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Transactional
    @CacheEvict(value = CACHE_BANNERS, allEntries = true)
    public void delete(Long id) {
        Banner banner = findByIdHelper(id);
        bannerRepository.delete(banner);
    }

    private Banner findByIdHelper(Long id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "Banner", id)));
    }

    private void setBannerFromRequest(Banner banner, BannerRequest request) {
        banner.setTitle(request.getTitle());
        banner.setSubtitle(request.getSubtitle());
        banner.setImageUrl(request.getImageUrl());
        banner.setLinkUrl(request.getLinkUrl());
        banner.setDisplayOrder(request.getDisplayOrder());
        banner.setActive(request.isActive());
    }
}
