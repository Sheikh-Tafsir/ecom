package com.example.ecom.cms.banner.dto;

import com.example.ecom.common.model.Banner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BannerDtoTest {

    @Nested
    @DisplayName("BannerRequest Tests")
    class BannerRequestTest {

        @Test
        @DisplayName("Setters and getters work correctly")
        void settersGetters_allFields_workCorrectly() {
            BannerRequest request = new BannerRequest();
            request.setTitle("Holiday Sale");
            request.setSubtitle("Up to 70% off");
            request.setImageUrl("https://cdn.example.com/holiday.jpg");
            request.setLinkUrl("/promotions/holiday");
            request.setDisplayOrder(2);
            request.setActive(false);

            assertEquals("Holiday Sale", request.getTitle());
            assertEquals("Up to 70% off", request.getSubtitle());
            assertEquals("https://cdn.example.com/holiday.jpg", request.getImageUrl());
            assertEquals("/promotions/holiday", request.getLinkUrl());
            assertEquals(2, request.getDisplayOrder());
            assertFalse(request.isActive());
        }

        @Test
        @DisplayName("Default active value is true")
        void defaults_newRequest_activeIsTrue() {
            BannerRequest request = new BannerRequest();
            assertTrue(request.isActive());
        }
    }

    @Nested
    @DisplayName("BannerResponse Tests")
    class BannerResponseTest {

        @Test
        @DisplayName("Canonical constructor sets all fields")
        void constructor_canonicalArgs_setsFields() {
            UUID id = UUID.fromString("01927d78-097f-7afa-9af3-182930415263");
            BannerResponse response = new BannerResponse(
                    id, "Flash Sale", "Limited time", "banner.jpg", "/sale", 1, true
            );

            assertEquals(id, response.id());
            assertEquals("Flash Sale", response.title());
            assertEquals("Limited time", response.subtitle());
            assertEquals("banner.jpg", response.imageUrl());
            assertEquals("/sale", response.linkUrl());
            assertEquals(1, response.displayOrder());
            assertTrue(response.active());
        }

        @Test
        @DisplayName("Construction from Banner entity maps all fields")
        void constructor_fromEntity_setsFields() {
            UUID id = UUID.fromString("01927d78-1a8f-7bfa-8af4-293041526374");
            Banner banner = new Banner();
            banner.setId(id);
            banner.setTitle("Winter Collection");
            banner.setSubtitle("New arrivals");
            banner.setImageUrl("https://cdn.example.com/winter.jpg");
            banner.setLinkUrl("/collections/winter");
            banner.setDisplayOrder(3);
            banner.setActive(true);

            BannerResponse response = new BannerResponse(banner);

            assertEquals(id, response.id());
            assertEquals("Winter Collection", response.title());
            assertEquals("New arrivals", response.subtitle());
            assertEquals("https://cdn.example.com/winter.jpg", response.imageUrl());
            assertEquals("/collections/winter", response.linkUrl());
            assertEquals(3, response.displayOrder());
            assertTrue(response.active());
        }
    }
}
