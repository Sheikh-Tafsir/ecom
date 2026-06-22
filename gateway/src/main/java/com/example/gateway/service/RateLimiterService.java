package com.example.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiterService {

    private static final String EMAIL_KEY_PREFIX = "email:global:";
    private static final String IP_KEY_PREFIX = "ip:global:";
    private static final int MAX_REQUESTS = 60;

    private static final String IP_AUTH_KEY_PREFIX = "ip:auth:";
    private static final int MAX_AUTH_REQUESTS = 10;

    private static final int WINDOW_SECONDS = 60;

    private static final String AUTH_URI= "/auth";

    private final ReactiveStringRedisTemplate redisTemplate;

    public Mono<Boolean> isEmailAllowed(String email) {
        return checkLimit(EMAIL_KEY_PREFIX + email, MAX_REQUESTS);
    }

    public Mono<Boolean> isIpAllowed(String ip, String uri) {
        if (uri.startsWith(AUTH_URI)) {
            return checkLimit(IP_AUTH_KEY_PREFIX + ip, MAX_AUTH_REQUESTS);
        }

        return checkLimit(IP_KEY_PREFIX + ip, MAX_REQUESTS);
    }

    private Mono<Boolean> checkLimit(String key, int requestLimit) {
        return redisTemplate.opsForValue()
                .increment(key)
                .flatMap(count -> {

                    boolean allowed = count <= requestLimit;

                    if (!allowed) {
                        log.warn(
                                "Rate limit exceeded for key={} count={} limit={}",
                                key,
                                count,
                                requestLimit
                        );
                    }

                    if (count == 1) {
                        return redisTemplate
                                .expire(key, Duration.ofSeconds(WINDOW_SECONDS))
                                .thenReturn(allowed);
                    }

                    return Mono.just(allowed);
                });
    }
}
