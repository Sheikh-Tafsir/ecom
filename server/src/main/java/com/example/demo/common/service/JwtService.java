package com.example.demo.common.service;

import com.example.demo.common.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    public final Long accessTokenValidity;

    public final Long refreshTokenValidity;

    private final Key accessTokenSecret;

    private final Key refreshTokenSecret;

    public JwtService(@Value("${access.token.validity}") Long accessTokenValidity, // 30 minutes
                      @Value("${refresh.token.validity}") Long refreshTokenValidity, // 7 days
                      @Value("${access.token.value}") String accessTokenSecret,
                      @Value("${refresh.token.value}") String refreshTokenSecret) {

        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
        this.accessTokenSecret = Keys.hmacShaKeyFor(accessTokenSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshTokenSecret = Keys.hmacShaKeyFor(refreshTokenSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("id", user.getId())
                .claim("name", user.getName())
                .claim("email", user.getEmail())
                .claim("role", user.getRoleValues())
                .claim("permissions", user.getPermissionValues())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenValidity))
                .signWith(accessTokenSecret)
                .compact();
    }

    public String generateRefreshToken(User user) {
        return generateRefreshToken(user, new Date(System.currentTimeMillis() + refreshTokenValidity));
    }

    public String generateRefreshToken(User user, Date expiration) {
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("id", user.getId())
                .setId(jti)
                .setIssuedAt(new Date())
                .setExpiration(expiration)
                .signWith(refreshTokenSecret)
                .compact();
    }

    public boolean isAccessTokenValid(String token) {
        return isTokenValid(token, accessTokenSecret);
    }

    public boolean isRefreshTokenValid(String token) {
        return isTokenValid(token, refreshTokenSecret);
    }

    public String getEmailFromAccessToken(String token) {
        return parseClaims(token, accessTokenSecret).getSubject();
    }

    public String getJtiFromRefreshToken(String token) {
        return parseClaims(token, refreshTokenSecret).getId();
    }

    public Date getExpirationFromRefreshToken(String token) {
        return parseClaims(token, refreshTokenSecret).getExpiration();
    }

    private boolean isTokenValid(String token, Key secretKey) {
        try {
            parseClaims(token, secretKey);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    private Claims parseClaims(String token, Key secretKey) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
