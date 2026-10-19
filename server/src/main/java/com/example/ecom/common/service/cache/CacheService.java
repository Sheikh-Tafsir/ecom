package com.example.ecom.common.service.cache;

import java.time.Duration;

public interface CacheService {

    String getToken(String key);

    void putToken(String key, String token, Duration ttl);
}
