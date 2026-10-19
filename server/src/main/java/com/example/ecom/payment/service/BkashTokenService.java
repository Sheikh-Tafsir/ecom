package com.example.ecom.payment.service;

import com.example.ecom.common.config.BkashConfigProperties;
import com.example.ecom.common.service.cache.CacheService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BkashTokenService {

    private static final String TOKEN_KEY = "bkash:id_token";
    private static final String REFRESH_KEY = "bkash:refresh_token";

    private final BkashConfigProperties config;
    private final CacheService cacheService;
    private final WebClient bkashWebClient;
    private final ObjectMapper objectMapper;

    /**
     * Returns a valid id_token, refreshing or re-granting as needed.
     */
    public String getValidToken() {
        String cached = cacheService.getToken(TOKEN_KEY);
        if (cached != null) {
            return cached;
        }

        String refresh = cacheService.getToken(REFRESH_KEY);
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

            // id_token valid for 1 hour, cache for 55 min to be safe
            cacheService.putToken(TOKEN_KEY, idToken, Duration.ofMinutes(55));
            // refresh_token valid for 28 days
            cacheService.putToken(REFRESH_KEY, refreshToken, Duration.ofDays(27));

            return idToken;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse bKash token response", e);
        }
    }

    private HttpHeaders baseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");
        return headers;
    }
}
