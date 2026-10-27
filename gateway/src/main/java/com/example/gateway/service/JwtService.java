package com.example.gateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
public class JwtService {

    private final SecretKey accessTokenSecret;

    public JwtService(@Value("${access.token.value}") String accessTokenSecret) {
        this.accessTokenSecret = Keys.hmacShaKeyFor(accessTokenSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Parses and validates an access token, enforcing mandatory JTI claim presence.
     *
     * @throws JwtException if the token is invalid, expired, or lacks a JTI claim
     */
    public Claims parseAccessTokenClaims(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(accessTokenSecret)
                .clockSkewSeconds(60)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (!StringUtils.hasText(claims.getId())) {
            throw new JwtException("Access token lacks mandatory JTI claim");
        }

        return claims;
    }
}
