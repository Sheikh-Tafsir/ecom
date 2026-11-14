package com.example.ecom.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

/**
 * L1/L2 failover cache decorator: tries Redis (L2) first,
 * falls back to Caffeine (L1) on failure. Writes always go to both.
 */
@Slf4j
public class ResilientCache implements Cache {

    private final Cache redisCache;
    private final Cache localCache;

    public ResilientCache(Cache redisCache, Cache localCache) {
        this.redisCache = redisCache;
        this.localCache = localCache;
    }

    @Override
    public String getName() {
        return redisCache.getName();
    }

    @Override
    public Object getNativeCache() {
        return redisCache.getNativeCache();
    }

    @Override
    public ValueWrapper get(Object key) {
        try {
            ValueWrapper val = redisCache.get(key);
            if (val != null) {
                localCache.put(key, val.get());
                return val;
            }
        } catch (Exception ex) {
            log.warn("Redis GET failed for cache [{}] key [{}]: {}. Falling back to L1.",
                    getName(), key, ex.getMessage());
        }
        return localCache.get(key);
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        try {
            T val = redisCache.get(key, type);
            if (val != null) {
                localCache.put(key, val);
                return val;
            }
        } catch (Exception ex) {
            log.warn("Redis GET failed for cache [{}] key [{}]: {}. Falling back to L1.",
                    getName(), key, ex.getMessage());
        }
        return localCache.get(key, type);
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        try {
            T val = redisCache.get(key, valueLoader);
            if (val != null) {
                localCache.put(key, val);
            }
            return val;
        } catch (Exception ex) {
            log.warn("Redis GET failed for cache [{}] key [{}]: {}. Falling back to L1.",
                    getName(), key, ex.getMessage());
            return localCache.get(key, valueLoader);
        }
    }

    @Override
    public void put(Object key, Object value) {
        try {
            redisCache.put(key, value);
        } catch (Exception ex) {
            log.warn("Redis PUT failed for cache [{}] key [{}]: {}. Writing to L1 only.",
                    getName(), key, ex.getMessage());
        }
        localCache.put(key, value);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        try {
            ValueWrapper existing = redisCache.putIfAbsent(key, value);
            if (existing != null) {
                localCache.put(key, existing.get());
            } else {
                localCache.put(key, value);
            }

            return existing;
        } catch (Exception ex) {
            log.warn("Redis PUT_IF_ABSENT failed for cache [{}] key [{}]: {}. Falling back to L1.",
                    getName(), key, ex.getMessage());
            return localCache.putIfAbsent(key, value);
        }
    }

    @Override
    public void evict(Object key) {
        try {
            redisCache.evict(key);
        } catch (Exception ex) {
            log.warn("Redis EVICT failed for cache [{}] key [{}]: {}.",
                    getName(), key, ex.getMessage());
        }

        localCache.evict(key);
    }

    @Override
    public boolean evictIfPresent(Object key) {
        boolean evicted = false;
        try {
            evicted = redisCache.evictIfPresent(key);
        } catch (Exception ex) {
            log.warn("Redis EVICT_IF_PRESENT failed for cache [{}] key [{}]: {}.",
                    getName(), key, ex.getMessage());
        }

        localCache.evictIfPresent(key);
        return evicted;
    }

    @Override
    public void clear() {
        try {
            redisCache.clear();
        } catch (Exception ex) {
            log.warn("Redis CLEAR failed for cache [{}]: {}.", getName(), ex.getMessage());
        }

        localCache.clear();
    }

    @Override
    public boolean invalidate() {
        boolean invalidated = false;

        try {
            invalidated = redisCache.invalidate();
        } catch (Exception ex) {
            log.warn("Redis INVALIDATE failed for cache [{}]: {}.", getName(), ex.getMessage());
        }

        localCache.invalidate();
        return invalidated;
    }
}
