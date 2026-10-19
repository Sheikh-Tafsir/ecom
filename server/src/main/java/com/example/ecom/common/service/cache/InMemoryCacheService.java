package com.example.ecom.common.service.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class InMemoryCacheService implements CacheService {

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private record CacheEntry(String token, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    @Override
    public String getToken(String key) {
        if (key == null) {
            return null;
        }

        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }

        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }

        return entry.token();
    }

    @Override
    public void putToken(String key, String token, Duration ttl) {
        if (key == null || token == null) {
            return;
        }

        Duration duration = (ttl != null && !ttl.isNegative() && !ttl.isZero()) ? ttl : Duration.ofMinutes(5);
        Instant expiresAt = Instant.now().plus(duration);
        cache.put(key, new CacheEntry(token, expiresAt));
    }

    public void clear() {
        cache.clear();
    }
}
