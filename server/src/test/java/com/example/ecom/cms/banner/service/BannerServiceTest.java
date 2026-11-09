package com.example.ecom.cms.banner.service;

import com.example.ecom.cms.banner.dto.BannerRequest;
import com.example.ecom.cms.banner.dto.BannerResponse;
import com.example.ecom.cms.banner.repository.BannerRepository;
import com.example.ecom.common.model.Banner;
import com.example.ecom.common.service.MessageService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BannerServiceTest {

    @Mock
    private BannerRepository bannerRepository;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private BannerService bannerService;

    private UUID bannerId;
    private Banner banner;
    private BannerRequest bannerRequest;

    @BeforeEach
    void setUp() {
        bannerId = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

        banner = new Banner();
        banner.setId(bannerId);
        banner.setTitle("Summer Sale");
        banner.setSubtitle("Up to 50% off");
        banner.setImageUrl("https://cdn.example.com/banners/summer.jpg");
        banner.setLinkUrl("https://example.com/summer");
        banner.setDisplayOrder(1);
        banner.setActive(true);

        bannerRequest = new BannerRequest();
        bannerRequest.setTitle("Summer Sale");
        bannerRequest.setSubtitle("Up to 50% off");
        bannerRequest.setImageUrl("https://cdn.example.com/banners/summer.jpg");
        bannerRequest.setLinkUrl("https://example.com/summer");
        bannerRequest.setDisplayOrder(1);
        bannerRequest.setActive(true);
    }

    @Test
    @DisplayName("findAllActive should return list of active banners ordered by display order")
    void findAllActive_activeBannersExist_returnsBannerResponseList() {
        Banner banner2 = new Banner();
        banner2.setId(UUID.randomUUID());
        banner2.setTitle("Winter Sale");
        banner2.setImageUrl("https://cdn.example.com/banners/winter.jpg");
        banner2.setDisplayOrder(2);
        banner2.setActive(true);

        when(bannerRepository.findAllByActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(banner, banner2));

        List<BannerResponse> result = bannerService.findAllActive();

        assertEquals(2, result.size());
        assertEquals("Summer Sale", result.get(0).title());
        assertEquals("Winter Sale", result.get(1).title());
        verify(bannerRepository).findAllByActiveTrueOrderByDisplayOrderAsc();
    }

    @Test
    @DisplayName("findAllActive should return empty list when no active banners exist")
    void findAllActive_noActiveBanners_returnsEmptyList() {
        when(bannerRepository.findAllByActiveTrueOrderByDisplayOrderAsc()).thenReturn(Collections.emptyList());

        List<BannerResponse> result = bannerService.findAllActive();

        assertTrue(result.isEmpty());
        verify(bannerRepository).findAllByActiveTrueOrderByDisplayOrderAsc();
    }

    @Test
    @DisplayName("findAll should return all banners including inactive ones")
    void findAll_bannersExist_returnsAllBanners() {
        Banner inactiveBanner = new Banner();
        inactiveBanner.setId(UUID.randomUUID());
        inactiveBanner.setTitle("Old Banner");
        inactiveBanner.setImageUrl("https://cdn.example.com/banners/old.jpg");
        inactiveBanner.setActive(false);

        when(bannerRepository.findAll()).thenReturn(List.of(banner, inactiveBanner));

        List<BannerResponse> result = bannerService.findAll();

        assertEquals(2, result.size());
        verify(bannerRepository).findAll();
    }

    @Test
    @DisplayName("findAll should return empty list when no banners exist")
    void findAll_noBanners_returnsEmptyList() {
        when(bannerRepository.findAll()).thenReturn(Collections.emptyList());

        List<BannerResponse> result = bannerService.findAll();

        assertTrue(result.isEmpty());
        verify(bannerRepository).findAll();
    }

    @Test
    @DisplayName("create should persist banner and return response when display order is set")
    void create_withDisplayOrder_savesBannerAndReturnsResponse() {
        when(bannerRepository.save(any(Banner.class))).thenReturn(banner);

        BannerResponse result = bannerService.create(bannerRequest);

        assertNotNull(result);
        assertEquals("Summer Sale", result.title());
        assertEquals("Up to 50% off", result.subtitle());
        assertEquals("https://cdn.example.com/banners/summer.jpg", result.imageUrl());
        assertEquals(1, result.displayOrder());
        assertTrue(result.active());
        verify(bannerRepository).save(any(Banner.class));
        verify(bannerRepository, never()).findMaxDisplayOrder();
    }

    @Test
    @DisplayName("create should auto-assign display order when it is zero")
    void create_withZeroDisplayOrder_autoAssignsDisplayOrder() {
        bannerRequest.setDisplayOrder(0);
        Banner savedBanner = new Banner();
        savedBanner.setId(bannerId);
        savedBanner.setTitle("Summer Sale");
        savedBanner.setImageUrl("https://cdn.example.com/banners/summer.jpg");
        savedBanner.setDisplayOrder(4);
        savedBanner.setActive(true);

        when(bannerRepository.findMaxDisplayOrder()).thenReturn(Optional.of(3));
        when(bannerRepository.save(any(Banner.class))).thenReturn(savedBanner);

        BannerResponse result = bannerService.create(bannerRequest);

        assertNotNull(result);
        verify(bannerRepository).findMaxDisplayOrder();
        verify(bannerRepository).save(any(Banner.class));
    }

    @Test
    @DisplayName("create should set display order to 1 when no banners exist and order is zero")
    void create_withZeroDisplayOrderAndNoBanners_setsDisplayOrderToOne() {
        bannerRequest.setDisplayOrder(0);
        Banner savedBanner = new Banner();
        savedBanner.setId(bannerId);
        savedBanner.setTitle("Summer Sale");
        savedBanner.setImageUrl("https://cdn.example.com/banners/summer.jpg");
        savedBanner.setDisplayOrder(1);
        savedBanner.setActive(true);

        when(bannerRepository.findMaxDisplayOrder()).thenReturn(Optional.empty());
        when(bannerRepository.save(any(Banner.class))).thenReturn(savedBanner);

        BannerResponse result = bannerService.create(bannerRequest);

        assertNotNull(result);
        verify(bannerRepository).findMaxDisplayOrder();
        verify(bannerRepository).save(any(Banner.class));
    }

    @Test
    @DisplayName("update should modify existing banner and return updated response")
    void update_existingBanner_updatesAndReturnsResponse() {
        bannerRequest.setTitle("Updated Summer Sale");
        Banner updatedBanner = new Banner();
        updatedBanner.setId(bannerId);
        updatedBanner.setTitle("Updated Summer Sale");
        updatedBanner.setImageUrl("https://cdn.example.com/banners/summer.jpg");
        updatedBanner.setDisplayOrder(1);
        updatedBanner.setActive(true);

        when(bannerRepository.findById(bannerId)).thenReturn(Optional.of(banner));
        when(bannerRepository.save(any(Banner.class))).thenReturn(updatedBanner);

        BannerResponse result = bannerService.update(bannerId, bannerRequest);

        assertNotNull(result);
        assertEquals("Updated Summer Sale", result.title());
        verify(bannerRepository).findById(bannerId);
        verify(bannerRepository).save(any(Banner.class));
    }

    @Test
    @DisplayName("update should throw EntityNotFoundException when banner not found")
    void update_nonExistentBanner_throwsEntityNotFoundException() {
        UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(bannerRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        when(messageService.get(eq("error.entity.not.found"), eq("Banner"), eq(nonExistentId)))
                .thenReturn("Banner not found with id: " + nonExistentId);

        assertThrows(EntityNotFoundException.class, () -> bannerService.update(nonExistentId, bannerRequest));

        verify(bannerRepository).findById(nonExistentId);
        verify(bannerRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete should remove existing banner")
    void delete_existingBanner_deletesBanner() {
        when(bannerRepository.findById(bannerId)).thenReturn(Optional.of(banner));

        bannerService.delete(bannerId);

        verify(bannerRepository).findById(bannerId);
        verify(bannerRepository).delete(banner);
    }

    @Test
    @DisplayName("delete should throw EntityNotFoundException when banner not found")
    void delete_nonExistentBanner_throwsEntityNotFoundException() {
        UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        when(bannerRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        when(messageService.get(eq("error.entity.not.found"), eq("Banner"), eq(nonExistentId)))
                .thenReturn("Banner not found with id: " + nonExistentId);

        assertThrows(EntityNotFoundException.class, () -> bannerService.delete(nonExistentId));

        verify(bannerRepository).findById(nonExistentId);
        verify(bannerRepository, never()).delete(any());
    }

    @Test
    @DisplayName("create should map all request fields correctly to banner entity")
    void create_withAllFields_mapsAllFieldsCorrectly() {
        bannerRequest.setSubtitle("Limited time offer");
        bannerRequest.setLinkUrl("https://example.com/promo");

        when(bannerRepository.save(any(Banner.class))).thenAnswer(invocation -> {
            Banner saved = invocation.getArgument(0);
            saved.setId(bannerId);
            assertEquals("Summer Sale", saved.getTitle());
            assertEquals("Limited time offer", saved.getSubtitle());
            assertEquals("https://cdn.example.com/banners/summer.jpg", saved.getImageUrl());
            assertEquals("https://example.com/promo", saved.getLinkUrl());
            assertEquals(1, saved.getDisplayOrder());
            assertTrue(saved.isActive());
            return saved;
        });

        bannerService.create(bannerRequest);

        verify(bannerRepository).save(any(Banner.class));
    }
}
