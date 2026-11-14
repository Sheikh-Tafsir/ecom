package com.example.ecom.common.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wraps every cache from the delegate in a {@link ResilientCache}
 * (L1 Caffeine + L2 Redis), except fail-closed caches which are
 * returned unwrapped.
 */
public class ResilientCacheManager implements CacheManager {

    private final CacheManager delegate;
    private final Duration defaultTtl;
    private final Set<String> failClosedCaches;
    private final Map<String, Duration> ttlMap;
    private final ConcurrentHashMap<String, Cache> resilientCaches = new ConcurrentHashMap<>();

    public ResilientCacheManager(CacheManager delegate,
                                 Map<String, Duration> ttlMap,
                                 Duration defaultTtl,
                                 Set<String> failClosedCaches) {
        this.delegate = delegate;
        this.ttlMap = ttlMap;
        this.defaultTtl = defaultTtl;
        this.failClosedCaches = failClosedCaches;
    }

    @Override
    public Cache getCache(String name) {
        if (failClosedCaches.contains(name)) {
            return delegate.getCache(name);
        }

        return resilientCaches.computeIfAbsent(name, n -> {
            Cache redisCache = delegate.getCache(n);
            if (redisCache == null) {
                return null;
            }

            Duration ttl = ttlMap.getOrDefault(n, defaultTtl);
            CaffeineCache localCache = new CaffeineCache(n,
                    Caffeine.newBuilder()
                            .expireAfterWrite(ttl)
                            .build());
            return new ResilientCache(redisCache, localCache);
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegate.getCacheNames();
    }
}
