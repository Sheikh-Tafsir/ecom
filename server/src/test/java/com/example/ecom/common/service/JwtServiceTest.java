package com.example.ecom.common.service;

import com.example.ecom.common.enums.UserStatus;
import com.example.ecom.common.model.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // Mock secrets must be at least 256 bits (32 bytes) for HS256
        String accessSecret = "my_super_secret_access_key_1234567890";
        String refreshSecret = "my_super_secret_refresh_key_1234567890";

        jwtService = new JwtService(
                accessSecret,
                1800,
                refreshSecret,
                604800,
                30
        );
    }

    @Test
    void testGenerateAndValidateAccessToken() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setName("Test User");
        user.setStatus(UserStatus.ACTIVE);

        String token = jwtService.generateAccessToken(user);

        assertNotNull(token);
        assertTrue(jwtService.isAccessTokenValid(token));
        assertEquals("test@example.com", jwtService.getEmailFromAccessToken(token));

        Claims claims = jwtService.parseAccessTokenClaims(token);
        assertEquals("1", claims.getSubject());
        assertEquals("1", claims.get("id").toString());
        assertEquals("Test User", claims.get("name"));
        assertEquals("test@example.com", claims.get("email"));
        assertNotNull(claims.getId());
    }

    @Test
    void testGenerateAndValidateRefreshToken() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        String refreshToken = jwtService.generateRefreshToken(user);

        assertNotNull(refreshToken);
        assertTrue(jwtService.isRefreshTokenValid(refreshToken));
        assertNotNull(jwtService.getJtiFromRefreshToken(refreshToken));
        assertNotNull(jwtService.getExpirationFromRefreshToken(refreshToken));
        assertTrue(jwtService.getExpirationFromRefreshToken(refreshToken).after(new Date()));
    }

    @Test
    void testGenerateSseAccessToken() {
        User user = new User();
        user.setId(1L);
        user.setEmail("sse@example.com");
        user.setStatus(UserStatus.ACTIVE);

        String sseToken = jwtService.generateSseAccessToken(user);

        assertNotNull(sseToken);
        assertTrue(jwtService.isAccessTokenValid(sseToken));
        assertEquals("sse@example.com", jwtService.getEmailFromAccessToken(sseToken));
    }

    @Test
    void testInvalidToken() {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature";
        assertFalse(jwtService.isAccessTokenValid(invalidToken));
        assertFalse(jwtService.isRefreshTokenValid(invalidToken));
    }

    @Test
    void testNullOrEmptyToken() {
        assertFalse(jwtService.isAccessTokenValid(null));
        assertFalse(jwtService.isAccessTokenValid(""));
        assertFalse(jwtService.isAccessTokenValid("   "));
        assertFalse(jwtService.isRefreshTokenValid(null));
        assertFalse(jwtService.isRefreshTokenValid(""));
        assertFalse(jwtService.isRefreshTokenValid("   "));
    }

    @Test
    void testGetJtiFromAccessToken() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        String token = jwtService.generateAccessToken(user);
        String jti = jwtService.getJtiFromAccessToken(token);

        assertNotNull(jti);
        assertFalse(jti.isBlank());
    }
}
