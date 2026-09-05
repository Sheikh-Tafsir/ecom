package com.example.gateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
public class JwtService {

    private final SecretKey accessTokenSecret;

    public JwtService(@Value("${access.token.value}") String accessTokenSecret) {
        this.accessTokenSecret = Keys.hmacShaKeyFor(accessTokenSecret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims parseAccessTokenClaims(String token) {
        return Jwts.parser()
                .verifyWith(accessTokenSecret)
                .clockSkewSeconds(60)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
