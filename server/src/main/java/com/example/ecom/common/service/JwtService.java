package com.example.ecom.common.service;

import com.example.ecom.common.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    public final long sseAccessTokenValidity;

    public JwtService(@Value("${jwt.access-token.secret:${access.token.value:}}") String accessTokenSecret,
                      @Value("${jwt.access-token.expiration:${access.token.validity:1800}}") long accessTokenValidity, // 30 minutes in seconds
                      @Value("${jwt.refresh-token.secret:${refresh.token.value:}}") String refreshTokenSecret,
                      @Value("${jwt.refresh-token.expiration:${refresh.token.validity:604800}}") long refreshTokenValidity, // 7 days in seconds
                      @Value("${jwt.sse-token.expiration:60}") long sseAccessTokenValidity // 60 seconds
    ) {
        this.accessTokenSecret = Keys.hmacShaKeyFor(accessTokenSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenSecret = Keys.hmacShaKeyFor(refreshTokenSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshTokenValidity = refreshTokenValidity;
        this.sseAccessTokenValidity = sseAccessTokenValidity;
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

    public String generateSseAccessToken(User user) {
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .setId(UUID.randomUUID().toString())
                .claim("id", user.getId())
                .claim("email", user.getEmail())
                .claim("status", user.getStatus())
                .claim("permissions", user.getPermissionValues())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + Duration.ofSeconds(sseAccessTokenValidity).toMillis()))
                .signWith(accessTokenSecret)
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

    public Claims parseAccessTokenClaims(String token) {
        return parseClaims(token, accessTokenSecret);
    }

    private Claims parseClaims(String token, Key secretKey) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
