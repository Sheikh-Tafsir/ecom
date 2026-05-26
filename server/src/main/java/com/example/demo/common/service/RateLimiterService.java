package com.example.demo.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private static final String LOGIN_EMAIL_KEY_PREFIX = "login:";
    private static final String API_EMAIL_KEY_PREFIX = "api:email:";
    private static final String API_IP_KEY_PREFIX = "api:ip:";

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int MAX_API_REQUESTS = 10;
    private static final int WINDOW_SECONDS = 60;

    private final StringRedisTemplate redisTemplate;

    public boolean isWithinLimit(Map<String, Integer> keysAndLimits) {
        for (Map.Entry<String, Integer> entry : keysAndLimits.entrySet()) {
            String key = entry.getKey();
            int maxLimit = entry.getValue();

            Long count = redisTemplate.opsForValue().increment(key);
            if (count == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(WINDOW_SECONDS));
            }

            if (count > maxLimit) {
                return false;
            }
        }

        return true;
    }

    public boolean isLoginAttemptAllowed(String email) {
        return isWithinLimit(Map.of(LOGIN_EMAIL_KEY_PREFIX + email, MAX_LOGIN_ATTEMPTS));
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
