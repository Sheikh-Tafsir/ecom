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

@Service
public class JwtService {

    public final Long accessTokenValidity; // 30 minutes

    public final Long refreshTokenValidity; // 7 days

    private final Key accessTokenSecret;

    private final Key refreshTokenSecret;

    public JwtService(@Value("${jwt.access.token.validity}") Long accessTokenValidity,
                      @Value("${jwt.refresh.token.validity}") Long refreshTokenValidity,
                      @Value("${jwt.access.token.value}") String accessTokenSecret,
                      @Value("${jwt.refresh.token.value}") String refreshTokenSecret) {

        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
        this.accessTokenSecret = Keys.hmacShaKeyFor(accessTokenSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshTokenSecret = Keys.hmacShaKeyFor(refreshTokenSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        return buildToken(user, accessTokenSecret, accessTokenValidity);
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, refreshTokenSecret, refreshTokenValidity);
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

    public String getEmailFromRefreshToken(String token) {
        return parseClaims(token, refreshTokenSecret).getSubject();
    }

    private String buildToken(User user, Key secretKey, long tokenValidity) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + tokenValidity))
                .signWith(secretKey)
                .compact();
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
