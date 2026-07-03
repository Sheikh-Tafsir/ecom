package com.example.gateway.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
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

    private Mono<Boolean> isAllowed(String key, int limit) {
        BucketConfiguration config = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(limit, Refill.intervally(limit, Duration.ofSeconds(WINDOW_SECONDS))))
                .build();

        return Mono.fromFuture(proxyManager.asAsync().builder().build(key.getBytes(), config).tryConsume(1));
    }

    public Mono<Boolean> isEmailAllowed(String email) {
        return isAllowed(EMAIL_KEY_PREFIX + email, MAX_REQUESTS);
    }

    public Mono<Boolean> isIpAllowed(String ip, String uri) {
        if (uri.startsWith(AUTH_URI)) {
            return isAllowed(IP_AUTH_KEY_PREFIX + ip, MAX_AUTH_REQUESTS);
        }

        return isAllowed(IP_KEY_PREFIX + ip, MAX_REQUESTS);
    }
}
