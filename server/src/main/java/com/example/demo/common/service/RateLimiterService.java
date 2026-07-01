package com.example.demo.common.service;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private static final String EMAIL_KEY_PREFIX = "email:global:";
    private static final String IP_KEY_PREFIX = "ip:global:";
    private static final int MAX_REQUESTS = 100;

    private static final String IP_AUTH_KEY_PREFIX = "ip:auth:";
    private static final int MAX_AUTH_REQUESTS = 20;

    private static final int WINDOW_SECONDS = 60;
    private static final String AUTH_URI= "/auth";

    private final ProxyManager<byte[]> proxyManager;

    private boolean isAllowed(String key, int limit) {
        BucketConfiguration config = BucketConfiguration.builder()
                .addLimit(limitBuilder -> limitBuilder
                        .capacity(limit)
                        .refillIntervally(limit, Duration.ofSeconds(WINDOW_SECONDS)))
                .build();

        Bucket bucket = proxyManager.builder().build(key.getBytes(), config);
        return bucket.tryConsume(1);
    }

    public boolean isEmailAllowed(String email) {
        return isAllowed(EMAIL_KEY_PREFIX + email, MAX_REQUESTS);
    }

    public boolean isIpAllowed(String ip, String uri) {
        if (uri.startsWith(AUTH_URI)) {
            return isAllowed(IP_AUTH_KEY_PREFIX + ip, MAX_AUTH_REQUESTS);
        }

        return isAllowed(IP_KEY_PREFIX + ip, MAX_REQUESTS);
    }
}
