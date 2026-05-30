package com.example.demo.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private static final String AUTH_EMAIL_KEY_PREFIX = "auth:email:";
    private static final String AUTH_IP_KEY_PREFIX = "auth:ip:";
    private static final String API_EMAIL_KEY_PREFIX = "api:email:";
    private static final String API_IP_KEY_PREFIX = "api:ip:";

    private static final int MAX_AUTH_REQUESTS = 3;
    private static final int MAX_API_REQUESTS = 20;
    private static final int WINDOW_SECONDS = 60;

    private final StringRedisTemplate redisTemplate;

    public boolean isWithinLimit(Map<String, Integer> limitsPerKey) {
        for (Map.Entry<String, Integer> entry : limitsPerKey.entrySet()) {

            String key = entry.getKey();
            int maxLimit = entry.getValue();

            Long count = redisTemplate.opsForValue().increment(key);

            if (count == null) {
                throw new IllegalStateException("Redis increment failed");
            }

            if (count == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(WINDOW_SECONDS));
            }

            if (count > maxLimit) {
                return false;
            }
        }

        return true;
    }

    public boolean isAuthRequestAllowed(String email, String ip) {
        if (email == null) {
            return isWithinLimit(Map.of(AUTH_IP_KEY_PREFIX + ip, MAX_AUTH_REQUESTS));
        }

        return isWithinLimit(Map.of(
                AUTH_EMAIL_KEY_PREFIX + email, MAX_AUTH_REQUESTS,
                AUTH_IP_KEY_PREFIX + ip, MAX_AUTH_REQUESTS
        ));
    }

    public boolean isApiRequestAllowed(String email, String ip) {
        if (email == null) {
            return isWithinLimit(Map.of(API_IP_KEY_PREFIX + ip, MAX_API_REQUESTS));
        }

        return isWithinLimit(Map.of(
                API_EMAIL_KEY_PREFIX + email, MAX_API_REQUESTS,
                API_IP_KEY_PREFIX + ip, MAX_API_REQUESTS
        ));
    }
}
