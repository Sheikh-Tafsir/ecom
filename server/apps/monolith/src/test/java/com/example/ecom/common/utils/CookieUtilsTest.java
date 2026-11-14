package com.example.ecom.common.utils;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class CookieUtilsTest {

    @Test
    @DisplayName("In production, cookie must have Secure=true and SameSite=None")
    void addCookie_whenProduction_setsSecureTrueAndSameSiteNone() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        CookieUtils.addCookie(response, "refreshToken", "secret-token-abc", 604800, true);

        String setCookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);

        assertTrue(setCookieHeader.contains("refreshToken=secret-token-abc"));
        assertTrue(setCookieHeader.contains("Secure"));
        assertTrue(setCookieHeader.contains("HttpOnly"));
        assertTrue(setCookieHeader.contains("SameSite=None"));
        assertTrue(setCookieHeader.contains("Path=/"));
        assertTrue(setCookieHeader.contains("Max-Age=604800"));
    }

    @Test
    @DisplayName("In development (non-production), cookie must have Secure=false and SameSite=Lax")
    void addCookie_whenNonProduction_setsSecureFalseAndSameSiteLax() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        CookieUtils.addCookie(response, "refreshToken", "dev-token-123", 3600, false);

        String setCookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);

        assertTrue(setCookieHeader.contains("refreshToken=dev-token-123"));
        assertFalse(setCookieHeader.contains("Secure"));
        assertTrue(setCookieHeader.contains("HttpOnly"));
        assertTrue(setCookieHeader.contains("SameSite=Lax"));
        assertTrue(setCookieHeader.contains("Path=/"));
        assertTrue(setCookieHeader.contains("Max-Age=3600"));
    }

    @Test
    @DisplayName("Clearing cookie on logout sets Max-Age=0 and empty value")
    void addCookie_whenLogout_clearsCookieWithZeroMaxAge() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        CookieUtils.addCookie(response, "refreshToken", null, 0, true);

        String setCookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);

        assertTrue(setCookieHeader.contains("refreshToken="));
        assertTrue(setCookieHeader.contains("Max-Age=0"));
        assertTrue(setCookieHeader.contains("Secure"));
        assertTrue(setCookieHeader.contains("SameSite=None"));
    }

    @Test
    @DisplayName("getCookieValue returns value when cookie exists")
    void getCookieValue_whenCookiePresent_returnsValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie("otherCookie", "otherValue"),
                new Cookie("refreshToken", "my-refresh-token")
        );

        String value = CookieUtils.getCookieValue(request, "refreshToken");
        assertEquals("my-refresh-token", value);
    }

    @Test
    @DisplayName("getCookieValue returns null when cookie is absent or cookies array is null")
    void getCookieValue_whenCookieAbsent_returnsNull() {
        MockHttpServletRequest requestWithCookies = new MockHttpServletRequest();
        requestWithCookies.setCookies(new Cookie("otherCookie", "val"));
        assertNull(CookieUtils.getCookieValue(requestWithCookies, "refreshToken"));

        MockHttpServletRequest emptyRequest = new MockHttpServletRequest();
        assertNull(CookieUtils.getCookieValue(emptyRequest, "refreshToken"));

        assertNull(CookieUtils.getCookieValue(null, "refreshToken"));
    }
}
