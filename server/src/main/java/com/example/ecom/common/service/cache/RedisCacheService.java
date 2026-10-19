package com.example.ecom.common.service.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@ConditionalOnBean(StringRedisTemplate.class)
@RequiredArgsConstructor
public class RedisCacheService implements CacheService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public String getToken(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void putToken(String key, String token, Duration ttl) {
        redisTemplate.opsForValue().set(key, token, ttl);
    }
}
