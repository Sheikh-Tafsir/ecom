package com.example.gateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "my-super-secret-key-for-testing-that-is-at-least-256-bits-long!!";
    private static final String USER_ID = "b7e1c2d3-4f5a-6b7c-8d9e-0f1a2b3c4d5e";
    private static final String USER_EMAIL = "alice.wonderland@example.com";

    private JwtService jwtService;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET);
        secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("parseAccessTokenClaims returns valid claims for a properly signed token with JTI")
    void parseAccessTokenClaims_validToken_returnsClaims() {
        String jti = UUID.randomUUID().toString();
        String token = buildToken(jti, USER_ID, USER_EMAIL, "Active", List.of("ROLE_USER"),
                Date.from(Instant.now().minus(1, ChronoUnit.MINUTES)),
                Date.from(Instant.now().plus(30, ChronoUnit.MINUTES)));

        Claims claims = jwtService.parseAccessTokenClaims(token);

        assertNotNull(claims);
        assertEquals(USER_ID, claims.getSubject());
        assertEquals(USER_EMAIL, claims.get("email", String.class));
        assertEquals(jti, claims.getId());
        assertEquals("Active", claims.get("status", String.class));
    }

    @Test
    @DisplayName("parseAccessTokenClaims throws JwtException for an expired token")
    void parseAccessTokenClaims_expiredToken_throwsJwtException() {
        String token = buildToken(UUID.randomUUID().toString(), USER_ID, USER_EMAIL, "Active", List.of("ROLE_USER"),
                Date.from(Instant.now().minus(10, ChronoUnit.MINUTES)),
                Date.from(Instant.now().minus(5, ChronoUnit.MINUTES)));

        assertThrows(JwtException.class, () -> jwtService.parseAccessTokenClaims(token));
    }

    @Test
    @DisplayName("parseAccessTokenClaims throws JwtException when JTI is missing")
    void parseAccessTokenClaims_missingJti_throwsJwtException() {
        String token = Jwts.builder()
                .subject(USER_ID)
                .claim("email", USER_EMAIL)
                .claim("status", "Active")
                .claim("permissions", List.of("ROLE_USER"))
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(30, ChronoUnit.MINUTES)))
                .signWith(secretKey)
                .compact();

        JwtException ex = assertThrows(JwtException.class, () -> jwtService.parseAccessTokenClaims(token));
        assertTrue(ex.getMessage().contains("JTI"));
    }

    @Test
    @DisplayName("parseAccessTokenClaims throws JwtException when JTI is an empty string")
    void parseAccessTokenClaims_emptyJti_throwsJwtException() {
        String token = Jwts.builder()
                .id("")
                .subject(USER_ID)
                .claim("email", USER_EMAIL)
                .claim("status", "Active")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(30, ChronoUnit.MINUTES)))
                .signWith(secretKey)
                .compact();

        JwtException ex = assertThrows(JwtException.class, () -> jwtService.parseAccessTokenClaims(token));
        assertTrue(ex.getMessage().contains("JTI"));
    }

    @Test
    @DisplayName("parseAccessTokenClaims throws exception for a tampered token")
    void parseAccessTokenClaims_tamperedToken_throwsException() {
        String token = buildToken(UUID.randomUUID().toString(), USER_ID, USER_EMAIL, "Active", List.of("ROLE_USER"),
                Date.from(Instant.now().minus(1, ChronoUnit.MINUTES)),
                Date.from(Instant.now().plus(30, ChronoUnit.MINUTES)));

        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

        assertThrows(Exception.class, () -> jwtService.parseAccessTokenClaims(tamperedToken));
    }

    @Test
    @DisplayName("parseAccessTokenClaims throws exception for a token signed with a different secret")
    void parseAccessTokenClaims_wrongSecret_throwsException() {
        SecretKey differentKey = Keys.hmacShaKeyFor(
                "different-secret-key-that-is-also-at-least-256-bits-long-for-hmac!!".getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(USER_ID)
                .claim("email", USER_EMAIL)
                .claim("status", "Active")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(30, ChronoUnit.MINUTES)))
                .signWith(differentKey)
                .compact();

        assertThrows(Exception.class, () -> jwtService.parseAccessTokenClaims(token));
    }

    @Test
    @DisplayName("parseAccessTokenClaims throws exception for a completely malformed token string")
    void parseAccessTokenClaims_malformedToken_throwsException() {
        assertThrows(Exception.class, () -> jwtService.parseAccessTokenClaims("not.a.valid.jwt"));
    }

    @Test
    @DisplayName("parseAccessTokenClaims throws exception for an empty token string")
    void parseAccessTokenClaims_emptyToken_throwsException() {
        assertThrows(Exception.class, () -> jwtService.parseAccessTokenClaims(""));
    }

    @Test
    @DisplayName("parseAccessTokenClaims extracts permissions list from claims")
    void parseAccessTokenClaims_withPermissions_extractsPermissions() {
        List<String> permissions = List.of("ROLE_ADMIN", "users:manage", "products:write");
        String token = buildToken(UUID.randomUUID().toString(), USER_ID, USER_EMAIL, "Active", permissions,
                Date.from(Instant.now().minus(1, ChronoUnit.MINUTES)),
                Date.from(Instant.now().plus(30, ChronoUnit.MINUTES)));

        Claims claims = jwtService.parseAccessTokenClaims(token);

        @SuppressWarnings("unchecked")
        List<String> extractedPermissions = claims.get("permissions", List.class);
        assertEquals(permissions, extractedPermissions);
    }

    private String buildToken(String jti, String subject, String email, String status,
                              List<String> permissions, Date issuedAt, Date expiration) {
        return Jwts.builder()
                .id(jti)
                .subject(subject)
                .claim("email", email)
                .claim("status", status)
                .claim("permissions", permissions)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }
}
