package com.example.ecom.common.service;

import com.example.ecom.common.enums.UserStatus;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        // Mock secrets must be at least 256 bits (32 bytes) for HS256
        String accessSecret = "my_super_secret_access_key_1234567890";
        String refreshSecret = "my_super_secret_refresh_key_1234567890";

        jwtService = new JwtService(
                accessSecret,
                1800,
                refreshSecret,
                604800
        );
    }

    @Test
    void testGenerateAndValidateAccessToken() {
        String token = jwtService.generateAccessToken(USER_ID, "Test User", "test@example.com", UserStatus.ACTIVE, List.of());

        assertNotNull(token);
        assertTrue(jwtService.isAccessTokenValid(token));
        assertEquals("test@example.com", jwtService.getEmailFromAccessToken(token));

        Claims claims = jwtService.parseAccessTokenClaims(token);
        assertEquals(USER_ID.toString(), claims.getSubject());
        assertEquals(USER_ID.toString(), claims.get("id").toString());
        assertEquals("Test User", claims.get("name"));
        assertEquals("test@example.com", claims.get("email"));
        assertNotNull(claims.getId());
    }

    @Test
    void testGenerateAndValidateRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken(USER_ID);

        assertNotNull(refreshToken);
        assertTrue(jwtService.isRefreshTokenValid(refreshToken));
        assertNotNull(jwtService.getJtiFromRefreshToken(refreshToken));
        assertNotNull(jwtService.getExpirationFromRefreshToken(refreshToken));
        assertTrue(jwtService.getExpirationFromRefreshToken(refreshToken).after(new Date()));
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
        String token = jwtService.generateAccessToken(USER_ID, "Test User", "test@example.com", UserStatus.ACTIVE, List.of());
        String jti = jwtService.getJtiFromAccessToken(token);

        assertNotNull(jti);
        assertFalse(jti.isBlank());
    }
}
