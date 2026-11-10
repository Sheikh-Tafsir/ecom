package com.example.ecom.payment.service;

import com.example.ecom.payment.config.BkashConfigProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

import static com.example.ecom.common.utils.CacheConstants.CACHE_BKASH_ID_TOKEN;
import static com.example.ecom.common.utils.CacheConstants.CACHE_BKASH_REFRESH_TOKEN;

@Slf4j
@Service
@RequiredArgsConstructor
public class BkashTokenService {

    private static final String CACHE_KEY = "token";

    private final BkashConfigProperties config;
    private final CacheManager cacheManager;
    private final WebClient bkashWebClient;
    private final ObjectMapper objectMapper;

    /**
     * Returns a valid id_token, refreshing or re-granting as needed.
     */
    public String getValidToken() {
        String cached = getCachedToken(CACHE_BKASH_ID_TOKEN);
        if (cached != null) {
            return cached;
        }

        String refresh = getCachedToken(CACHE_BKASH_REFRESH_TOKEN);
        if (refresh != null) {
            return refreshToken(refresh);
        }

        return grantToken();
    }

    private String refreshToken(String refreshToken) {
        try {
            HttpHeaders headers = baseHeaders();
            headers.set("username", config.getUsername());
            headers.set("password", config.getPassword());

            Map<String, String> body = Map.of(
                    "app_key", config.getAppKey(),
                    "app_secret", config.getAppSecret(),
                    "refresh_token", refreshToken
            );

            String responseBody = bkashWebClient.post()
                    .uri(config.getBaseUrl() + "/token/refresh")
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return extractAndCacheTokens(responseBody);
        } catch (Exception e) {
            log.warn("Refresh failed, re-granting token", e);
            return grantToken();
        }
    }

    private String grantToken() {
        HttpHeaders headers = baseHeaders();
        headers.set("username", config.getUsername());
        headers.set("password", config.getPassword());

        Map<String, String> body = Map.of(
                "app_key", config.getAppKey(),
                "app_secret", config.getAppSecret()
        );

        String responseBody = bkashWebClient.post()
                .uri(config.getBaseUrl() + "/token/grant")
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return extractAndCacheTokens(responseBody);
    }

    private String extractAndCacheTokens(String responseBody) {
        log.debug("bKash token response: {}", responseBody);
        try {
            JsonNode node = objectMapper.readTree(responseBody);
            String idToken = node.path("id_token").asText();
            String refreshToken = node.path("refresh_token").asText();

            if (idToken.isEmpty()) {
                throw new RuntimeException("id_token is missing from response");
            }

            putCachedToken(CACHE_BKASH_ID_TOKEN, idToken);
            if (!refreshToken.isEmpty()) {
                putCachedToken(CACHE_BKASH_REFRESH_TOKEN, refreshToken);
            }

            return idToken;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse bKash token response", e);
        }
    }

    private String getCachedToken(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        return cache != null ? cache.get(CACHE_KEY, String.class) : null;
    }

    private void putCachedToken(String cacheName, String token) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(CACHE_KEY, token);
        }
    }

    private HttpHeaders baseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");
        return headers;
    }
}
