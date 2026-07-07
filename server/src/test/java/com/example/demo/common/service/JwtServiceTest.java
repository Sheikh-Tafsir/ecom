package com.example.demo.common.service;

import com.example.demo.common.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // Mock secrets must be at least 256 bits (32 bytes) for HS256
        String accessSecret = "my_super_secret_access_key_1234567890";
        String refreshSecret = "my_super_secret_refresh_key_1234567890";
        
        jwtService = new JwtService(
                1800000L, 
                604800000L, 
                accessSecret, 
                refreshSecret
        );
    }

    @Test
    void testGenerateAndValidateAccessToken() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setName("Test User");

        String token = jwtService.generateAccessToken(user);
        
        assertNotNull(token);
        assertTrue(jwtService.isAccessTokenValid(token));
        assertEquals("test@example.com", jwtService.getEmailFromAccessToken(token));
    }

    @Test
    void testInvalidToken() {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature";
        assertFalse(jwtService.isAccessTokenValid(invalidToken));
    }
}
