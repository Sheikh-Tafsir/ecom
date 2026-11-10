package com.example.ecom.cms.banner.validator;

import com.example.ecom.cms.banner.dto.BannerRequest;
import com.example.ecom.cms.banner.repository.BannerRepository;
import com.example.ecom.cms.banner.model.Banner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BannerValidatorTest {

    @Mock
    private BannerRepository bannerRepository;

    @InjectMocks
    private BannerValidator bannerValidator;

    private BannerRequest bannerRequest;
    private Errors errors;

    @BeforeEach
    void setUp() throws Exception {
        Field maxTotalField = BannerValidator.class.getDeclaredField("maxTotalBanners");
        maxTotalField.setAccessible(true);
        maxTotalField.setInt(bannerValidator, 20);

        Field maxActiveField = BannerValidator.class.getDeclaredField("maxActiveBanners");
        maxActiveField.setAccessible(true);
        maxActiveField.setInt(bannerValidator, 5);
        bannerRequest = new BannerRequest();
        bannerRequest.setTitle("Flash Sale Banner");
        bannerRequest.setImageUrl("https://cdn.example.com/banners/flash.jpg");
        bannerRequest.setDisplayOrder(3);
        bannerRequest.setActive(true);
        errors = new BeanPropertyBindingResult(bannerRequest, "bannerRequest");
    }

    // ── validateCreate ──────────────────────────────────────────

    @Test
    @DisplayName("validateCreate should pass when all limits are within bounds and display order is unique")
    void validateCreate_validRequest_noErrors() {
        when(bannerRepository.count()).thenReturn(5L);
        when(bannerRepository.countByActiveTrue()).thenReturn(2L);
        when(bannerRepository.existsByDisplayOrder(3)).thenReturn(false);

        bannerValidator.validateCreate(bannerRequest, errors);

        assertFalse(errors.hasErrors());
        verify(bannerRepository).count();
        verify(bannerRepository).countByActiveTrue();
        verify(bannerRepository).existsByDisplayOrder(3);
    }

    @Test
    @DisplayName("validateCreate should reject when total banner limit is exceeded")
    void validateCreate_totalLimitExceeded_rejectsGlobal() {
        when(bannerRepository.count()).thenReturn(20L);
        when(bannerRepository.countByActiveTrue()).thenReturn(2L);
        when(bannerRepository.existsByDisplayOrder(3)).thenReturn(false);

        bannerValidator.validateCreate(bannerRequest, errors);

        assertTrue(errors.hasGlobalErrors());
        assertEquals("error.limit.exceeded", errors.getGlobalErrors().get(0).getCode());
        verify(bannerRepository).count();
    }

    @Test
    @DisplayName("validateCreate should reject when active banner limit is exceeded for an active request")
    void validateCreate_activeLimitExceeded_rejectsActiveField() {
        when(bannerRepository.count()).thenReturn(10L);
        when(bannerRepository.countByActiveTrue()).thenReturn(5L);
        when(bannerRepository.existsByDisplayOrder(3)).thenReturn(false);

        bannerValidator.validateCreate(bannerRequest, errors);

        assertTrue(errors.hasFieldErrors("active"));
        assertEquals("error.limit.exceeded", errors.getFieldError("active").getCode());
        verify(bannerRepository).countByActiveTrue();
    }

    @Test
    @DisplayName("validateCreate should not check active limit when request is inactive")
    void validateCreate_inactiveRequest_skipsActiveCheck() {
        bannerRequest.setActive(false);
        when(bannerRepository.count()).thenReturn(10L);
        when(bannerRepository.existsByDisplayOrder(3)).thenReturn(false);

        bannerValidator.validateCreate(bannerRequest, errors);

        assertFalse(errors.hasErrors());
        verify(bannerRepository, never()).countByActiveTrue();
    }

    @Test
    @DisplayName("validateCreate should reject when display order already exists")
    void validateCreate_duplicateDisplayOrder_rejectsDisplayOrderField() {
        when(bannerRepository.count()).thenReturn(10L);
        when(bannerRepository.countByActiveTrue()).thenReturn(2L);
        when(bannerRepository.existsByDisplayOrder(3)).thenReturn(true);

        bannerValidator.validateCreate(bannerRequest, errors);

        assertTrue(errors.hasFieldErrors("displayOrder"));
        assertEquals("error.field.duplicate", errors.getFieldError("displayOrder").getCode());
        verify(bannerRepository).existsByDisplayOrder(3);
    }

    @Test
    @DisplayName("validateCreate should skip display order check when order is zero")
    void validateCreate_displayOrderZero_skipsDisplayOrderCheck() {
        bannerRequest.setDisplayOrder(0);
        when(bannerRepository.count()).thenReturn(10L);
        when(bannerRepository.countByActiveTrue()).thenReturn(2L);

        bannerValidator.validateCreate(bannerRequest, errors);

        assertFalse(errors.hasFieldErrors("displayOrder"));
        verify(bannerRepository, never()).existsByDisplayOrder(anyInt());
    }

    @Test
    @DisplayName("validateCreate should report multiple errors simultaneously")
    void validateCreate_multipleViolations_reportsAllErrors() {
        when(bannerRepository.count()).thenReturn(20L);
        when(bannerRepository.countByActiveTrue()).thenReturn(5L);
        when(bannerRepository.existsByDisplayOrder(3)).thenReturn(true);

        bannerValidator.validateCreate(bannerRequest, errors);

        assertTrue(errors.hasGlobalErrors());
        assertTrue(errors.hasFieldErrors("active"));
        assertTrue(errors.hasFieldErrors("displayOrder"));
    }

    // ── validateUpdate ──────────────────────────────────────────

    @Test
    @DisplayName("validateUpdate should pass when all validations pass")
    void validateUpdate_validRequest_noErrors() {
        UUID bannerId = UUID.fromString("b1c2d3e4-f5a6-7890-bcde-f12345678901");
        bannerRequest.setActive(false);
        when(bannerRepository.existsByDisplayOrderAndIdNot(3, bannerId)).thenReturn(false);

        bannerValidator.validateUpdate(bannerId, bannerRequest, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("validateUpdate should reject when activating a banner exceeds active limit")
    void validateUpdate_activatingExceedsLimit_rejectsActiveField() {
        UUID bannerId = UUID.fromString("b1c2d3e4-f5a6-7890-bcde-f12345678901");
        Banner existingBanner = new Banner();
        existingBanner.setId(bannerId);
        existingBanner.setActive(false);

        when(bannerRepository.findById(bannerId)).thenReturn(Optional.of(existingBanner));
        when(bannerRepository.countByActiveTrue()).thenReturn(5L);
        when(bannerRepository.existsByDisplayOrderAndIdNot(3, bannerId)).thenReturn(false);

        bannerValidator.validateUpdate(bannerId, bannerRequest, errors);

        assertTrue(errors.hasFieldErrors("active"));
        assertEquals("error.limit.exceeded", errors.getFieldError("active").getCode());
    }

    @Test
    @DisplayName("validateUpdate should allow activating when banner was already active")
    void validateUpdate_alreadyActive_noActiveError() {
        UUID bannerId = UUID.fromString("b1c2d3e4-f5a6-7890-bcde-f12345678901");
        Banner existingBanner = new Banner();
        existingBanner.setId(bannerId);
        existingBanner.setActive(true);

        when(bannerRepository.findById(bannerId)).thenReturn(Optional.of(existingBanner));
        when(bannerRepository.existsByDisplayOrderAndIdNot(3, bannerId)).thenReturn(false);

        bannerValidator.validateUpdate(bannerId, bannerRequest, errors);

        assertFalse(errors.hasFieldErrors("active"));
    }

    @Test
    @DisplayName("validateUpdate should reject when display order conflicts with another banner")
    void validateUpdate_duplicateDisplayOrder_rejectsDisplayOrderField() {
        UUID bannerId = UUID.fromString("b1c2d3e4-f5a6-7890-bcde-f12345678901");
        bannerRequest.setActive(false);
        when(bannerRepository.existsByDisplayOrderAndIdNot(3, bannerId)).thenReturn(true);

        bannerValidator.validateUpdate(bannerId, bannerRequest, errors);

        assertTrue(errors.hasFieldErrors("displayOrder"));
        assertEquals("error.field.duplicate", errors.getFieldError("displayOrder").getCode());
    }

    @Test
    @DisplayName("validateUpdate should skip display order check when order is zero")
    void validateUpdate_displayOrderZero_skipsDisplayOrderCheck() {
        UUID bannerId = UUID.fromString("b1c2d3e4-f5a6-7890-bcde-f12345678901");
        bannerRequest.setDisplayOrder(0);
        bannerRequest.setActive(false);

        bannerValidator.validateUpdate(bannerId, bannerRequest, errors);

        assertFalse(errors.hasFieldErrors("displayOrder"));
        verify(bannerRepository, never()).existsByDisplayOrderAndIdNot(anyInt(), any());
    }

    @Test
    @DisplayName("validateUpdate should not reject active when banner id is not found")
    void validateUpdate_bannerNotFound_noActiveError() {
        UUID bannerId = UUID.fromString("b1c2d3e4-f5a6-7890-bcde-f12345678901");
        when(bannerRepository.findById(bannerId)).thenReturn(Optional.empty());
        when(bannerRepository.existsByDisplayOrderAndIdNot(3, bannerId)).thenReturn(false);

        bannerValidator.validateUpdate(bannerId, bannerRequest, errors);

        assertFalse(errors.hasFieldErrors("active"));
    }
}
