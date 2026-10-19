package com.example.ecom.common.service.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Primary
@Service
public class ResilientCacheService implements CacheService {

    public static final String CACHE_STRATEGY_REDIS = "redis";

    private final RedisCacheService redisService;
    private final InMemoryCacheService inMemoryService;
    private final String cacheStrategy;

    @Autowired
    public ResilientCacheService(@Nullable RedisCacheService redisService,
                                 InMemoryCacheService inMemoryService,
                                 @Value("${app.cache.strategy:redis}") String cacheStrategy) {
        this.redisService = redisService;
        this.inMemoryService = inMemoryService;
        this.cacheStrategy = cacheStrategy;
    }

    public ResilientCacheService(RedisCacheService redisService, InMemoryCacheService inMemoryService) {
        this(redisService, inMemoryService, CACHE_STRATEGY_REDIS);
    }

    @Override
    public String getToken(String key) {
        if (CACHE_STRATEGY_REDIS.equalsIgnoreCase(cacheStrategy)) {
            if (redisService != null) {
                try {
                    String token = redisService.getToken(key);
                    if (token != null) {
                        return token;
                    }
                } catch (Exception ex) {
                    log.warn("Cache strategy is 'redis', but Redis operation failed for key [{}]: {}. Falling back to in-memory cache.", key, ex.getMessage());
                }
            } else {
                log.warn("Cache strategy is 'redis', but Redis is unavailable. Falling back to in-memory cache.");
            }
        }

        return inMemoryService.getToken(key);
    }

    @Override
    public void putToken(String key, String token, Duration ttl) {
        if (CACHE_STRATEGY_REDIS.equalsIgnoreCase(cacheStrategy)) {
            if (redisService != null) {
                try {
                    redisService.putToken(key, token, ttl);
                } catch (Exception ex) {
                    log.warn("Cache strategy is 'redis', but Redis operation failed for key [{}]: {}. Falling back to in-memory cache.", key, ex.getMessage());
                }
            } else {
                log.warn("Cache strategy is 'redis', but Redis is unavailable. Falling back to in-memory cache.");
            }
        }

        inMemoryService.putToken(key, token, ttl);
    }
}
