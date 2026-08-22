package com.example.ecom.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import static com.example.ecom.common.utils.CacheConstants.CACHE_IDEMPOTENCY;
import static com.example.ecom.common.utils.Utils.objectHash;

@Component
@RequiredArgsConstructor
public class IdempotencyService {

    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final CacheManager cacheManager;

    public Object getCachedResponse(String idempotencyKey, Object request) {
        Cache cache = getCache();
        return cache != null ? cache.get(getKey(idempotencyKey, request), Object.class) : null;
    }

    public void save(String idempotencyKey, Object request, Object response) {
        Cache cache = getCache();
        if (cache != null) {
            cache.put(getKey(idempotencyKey, request), response);
        }
    }

    public void remove(String idempotencyKey, Object request) {
        Cache cache = getCache();
        if (cache != null) {
            cache.evict(getKey(idempotencyKey, request));
        }
    }

    private Cache getCache() {
        return cacheManager.getCache(CACHE_IDEMPOTENCY);
    }

    private String getKey(String idempotencyKey, Object request) {
        return idempotencyKey + ":" + objectHash(request);
    }
}
