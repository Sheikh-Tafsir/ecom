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

    private static final String EMAIL_KEY_PREFIX = "email:";
    private static final String IP_KEY_PREFIX = "ip:";
    private static final int MAX_REQUESTS = 60;
    private static final int WINDOW_SECONDS = 60;

    private final ReactiveStringRedisTemplate redisTemplate;

    public Mono<Boolean> isRequestAllowed(String email, String ip) {
        Mono<Boolean> ipAllowed = checkLimit(IP_KEY_PREFIX + ip);

        if (email == null) {
            return ipAllowed;
        }

        Mono<Boolean> emailAllowed = checkLimit(EMAIL_KEY_PREFIX + email);

        return Mono.zip(ipAllowed, emailAllowed)
                .map(tuple -> tuple.getT1() && tuple.getT2());
    }

    private Mono<Boolean> checkLimit(String key) {
        return redisTemplate.opsForValue()
                .increment(key)
                .flatMap(count -> {

                    boolean allowed = count <= MAX_REQUESTS;

                    if (!allowed) {
                        log.error(
                                "Rate limit exceeded for key={} count={} limit={}",
                                key,
                                count,
                                MAX_REQUESTS
                        );
                    }

                    Mono<Boolean> result = Mono.just(allowed);

                    if (count == 1) {
                        log.debug(
                                "Created rate limit window for key={} duration={}s",
                                key,
                                WINDOW_SECONDS
                        );

                        return redisTemplate
                                .expire(key, Duration.ofSeconds(WINDOW_SECONDS))
                                .then(result);
                    }

                    return result;
                });
    }
}
