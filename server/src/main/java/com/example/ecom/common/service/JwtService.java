package com.example.ecom.common.service;

import com.example.ecom.common.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final Key accessTokenSecret;

    public final long accessTokenValidity;

    private final Key refreshTokenSecret;

    public final long refreshTokenValidity;

    public JwtService(@Value("${jwt.access-token.secret}") String accessTokenSecret,
                      @Value("${jwt.access-token.expiration:900}") long accessTokenValidity, // in seconds
                      @Value("${jwt.refresh-token.secret}") String refreshTokenSecret,
                      @Value("${jwt.refresh-token.expiration:604800}") long refreshTokenValidity // in seconds
    ) {
        this.accessTokenSecret = Keys.hmacShaKeyFor(accessTokenSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenSecret = Keys.hmacShaKeyFor(refreshTokenSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshTokenValidity = refreshTokenValidity;
    }

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .setId(UUID.randomUUID().toString())
                .claim("id", user.getId())
                .claim("name", user.getName())
                .claim("email", user.getEmail())
                .claim("status", user.getStatus())
                .claim("permissions", user.getPermissionValues())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + Duration.ofSeconds(accessTokenValidity).toMillis()))
                .signWith(accessTokenSecret)
                .compact();
    }

    public String generateRefreshToken(User user) {
        return generateRefreshToken(user, new Date(System.currentTimeMillis() + Duration.ofSeconds(refreshTokenValidity).toMillis()));
    }

    public String generateRefreshToken(User user, Date expiration) {
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .setId(jti)
                .setIssuedAt(new Date())
                .setExpiration(expiration)
                .signWith(refreshTokenSecret)
                .compact();
    }

    public boolean isAccessTokenValid(String token) {
        try {
            parseAccessTokenClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getEmailFromAccessToken(String token) {
        return parseAccessTokenClaims(token).get("email", String.class);
    }

    public String getJtiFromAccessToken(String token) {
        return parseAccessTokenClaims(token).getId();
    }

    public boolean isRefreshTokenValid(String token) {
        try {
            parseClaims(token, refreshTokenSecret);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getJtiFromRefreshToken(String token) {
        return parseClaims(token, refreshTokenSecret).getId();
    }

    public Date getExpirationFromRefreshToken(String token) {
        return parseClaims(token, refreshTokenSecret).getExpiration();
    }

    /**
     * Parses and validates an access token.
     *
     * @throws JwtException if the token is invalid, expired, or lacks a JTI claim
     */
    public Claims parseAccessTokenClaims(String token) {
        return parseClaims(token, accessTokenSecret);
    }

    /**
     * Parses and validates a JWT token, enforcing mandatory JTI claim presence.
     *
     * @throws JwtException if the token is invalid, expired, or lacks a JTI claim
     */
    private Claims parseClaims(String token, Key secretKey) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .setAllowedClockSkewSeconds(60)
                .build()
                .parseClaimsJws(token)
                .getBody();

        if (!StringUtils.hasText(claims.getId())) {
            throw new JwtException("Token lacks mandatory JTI claim");
        }

        return claims;
    }
}
