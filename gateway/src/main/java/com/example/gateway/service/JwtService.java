package com.example.gateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Service
public class JwtService {

    private final Key accessTokenSecret;

    public JwtService(@Value("${access.token.value}") String accessTokenSecret) {
        this.accessTokenSecret = Keys.hmacShaKeyFor(accessTokenSecret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims parseAccessTokenClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(accessTokenSecret)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
