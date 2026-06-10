package com.example.demo.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static com.example.demo.common.utils.Utils.objectHash;

@Component
@RequiredArgsConstructor
public class IdempotencyService {

    public static final int VALIDITY_INT_MINUTES = 60;

    public static final String IDEMPOTENCY_HEADER= "Idempotency-Key";

    private final RedisTemplate<String, Object> redisTemplate;

    public Object getCachedResponse(String idempotencyKey, Object request) {
        return redisTemplate.opsForValue().get(getKey(idempotencyKey, request));
    }

    public void save(String idempotencyKey, Object request, Object response) {
        redisTemplate.opsForValue().set(getKey(idempotencyKey, request), response, Duration.ofMinutes(VALIDITY_INT_MINUTES));
    }

    public void remove(String idempotencyKey, Object request) {
        redisTemplate.delete(getKey(idempotencyKey, request));
    }

    private String getKey(String idempotencyKey, Object request) {
        return "Idempotency:" + idempotencyKey + ":" + objectHash(request);
    }
}
