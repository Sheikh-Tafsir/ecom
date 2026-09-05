package com.example.gateway.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    public static final String RATE_LIMIT_PREFIX = "rate_limit:";
    public static final String CONCURRENCY_LIMIT_PREFIX = "concurrency_limit:";
    public static final String GLOBAL_SCOPE = "global";
    public static final String IP_SCOPE_PREFIX = "ip:";
    public static final String AUTH_SCOPE_PREFIX = "auth_ip:";
    public static final String USER_SCOPE_PREFIX = "user:";

    public static final Set<String> RATE_LIMIT_EXCLUDED_PATHS = Set.of(
            "/socket.io",
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs"
    );

    @Value("${rate-limit.global.rate:2000}")
    private int globalRateLimit;

    @Value("${rate-limit.global.concurrency:100}")
    private int globalConcurrencyLimit;

    @Value("${rate-limit.ip.rate:100}")
    private int ipRateLimit;

    @Value("${rate-limit.ip.concurrency:20}")
    private int ipConcurrencyLimit;

    @Value("${rate-limit.auth-ip.rate:20}")
    private int authIpRateLimit;

    @Value("${rate-limit.auth-ip.concurrency:5}")
    private int authIpConcurrencyLimit;

    @Value("${rate-limit.user.rate:300}")
    private int userRateLimit;

    @Value("${rate-limit.user.concurrency:15}")
    private int userConcurrencyLimit;

    @Value("${rate-limit.window-seconds:60}")
    private int windowSeconds;

    private final Optional<ProxyManager<byte[]>> proxyManager;

    @Autowired(required = false)
    private ReactiveStringRedisTemplate redisTemplate;

    private final Map<Integer, BucketConfiguration> configCache = new ConcurrentHashMap<>();

    // Local Fallbacks
    private final Cache<String, Bucket> localBuckets = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(10000)
            .build();

    private final Cache<String, AtomicInteger> localConcurrency = Caffeine.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .maximumSize(5000)
            .build();

    // --- Rate Limiting (Frequency) ---

    public Mono<Boolean> isGlobalRateAllowed() {
        return isRateAllowed(RATE_LIMIT_PREFIX + GLOBAL_SCOPE, globalRateLimit);
    }

    public Mono<Boolean> isIpRateAllowed(String ip, boolean isAuth) {
        String key = isAuth
                ? RATE_LIMIT_PREFIX + AUTH_SCOPE_PREFIX + ip
                : RATE_LIMIT_PREFIX + IP_SCOPE_PREFIX + ip;
        int limit = isAuth ? authIpRateLimit : ipRateLimit;
        return isRateAllowed(key, limit);
    }

    public Mono<Boolean> isUserRateAllowed(String email) {
        return isRateAllowed(RATE_LIMIT_PREFIX + USER_SCOPE_PREFIX + email, userRateLimit);
    }

    private Mono<Boolean> isRateAllowed(String key, int limit) {
        BucketConfiguration config = configCache.computeIfAbsent(limit, l -> BucketConfiguration.builder()
                .addLimit(Bandwidth.builder().capacity(l).refillIntervally(l, Duration.ofSeconds(windowSeconds)).build())
                .build());

        if (proxyManager.isPresent()) {
            return Mono.fromFuture(proxyManager.get().asAsync().builder().build(key.getBytes(), config).tryConsume(1))
                    .onErrorResume(e -> {
                        log.warn("Distributed rate limiter error for key {}, falling back to local: {}", key, e.getMessage());
                        Bucket bucket = localBuckets.get(key, k -> Bucket.builder().addLimit(config.getBandwidths()[0]).build());
                        return Mono.just(bucket.tryConsume(1));
                    });
        }

        Bucket bucket = localBuckets.get(key, k -> Bucket.builder().addLimit(config.getBandwidths()[0]).build());
        return Mono.just(bucket.tryConsume(1));
    }

    // --- Concurrency Throttling (Bulkhead) ---

    public Mono<Boolean> acquireGlobalConcurrency() {
        return acquire(CONCURRENCY_LIMIT_PREFIX + GLOBAL_SCOPE, globalConcurrencyLimit);
    }

    public Mono<Void> releaseGlobalConcurrency() {
        return release(CONCURRENCY_LIMIT_PREFIX + GLOBAL_SCOPE);
    }

    public Mono<Boolean> acquireIpConcurrency(String ip, boolean isAuth) {
        String key = isAuth
                ? CONCURRENCY_LIMIT_PREFIX + AUTH_SCOPE_PREFIX + ip
                : CONCURRENCY_LIMIT_PREFIX + IP_SCOPE_PREFIX + ip;
        int limit = isAuth ? authIpConcurrencyLimit : ipConcurrencyLimit;
        return acquire(key, limit);
    }

    public Mono<Void> releaseIpConcurrency(String ip, boolean isAuth) {
        String key = isAuth
                ? CONCURRENCY_LIMIT_PREFIX + AUTH_SCOPE_PREFIX + ip
                : CONCURRENCY_LIMIT_PREFIX + IP_SCOPE_PREFIX + ip;
        return release(key);
    }

    public Mono<Boolean> acquireUserConcurrency(String email) {
        return acquire(CONCURRENCY_LIMIT_PREFIX + USER_SCOPE_PREFIX + email, userConcurrencyLimit);
    }

    public Mono<Void> releaseUserConcurrency(String email) {
        return release(CONCURRENCY_LIMIT_PREFIX + USER_SCOPE_PREFIX + email);
    }

    private Mono<Boolean> acquire(String key, int limit) {
        if (redisTemplate != null) {
            return redisTemplate.opsForValue().increment(key)
                    .flatMap(current -> {
                        if (current > limit) {
                            return redisTemplate.opsForValue().decrement(key).thenReturn(false);
                        }
                        // Always refresh TTL as a safety net against counter leaks from crashes
                        return redisTemplate.expire(key, Duration.ofMinutes(5)).thenReturn(true);
                    })
                    .onErrorResume(e -> {
                        log.warn("Redis concurrency acquire error for key {}, falling back to local: {}", key, e.getMessage());
                        return Mono.just(acquireLocal(key, limit));
                    });
        }

        return Mono.just(acquireLocal(key, limit));
    }

    private Mono<Void> release(String key) {
        if (redisTemplate != null) {
            return redisTemplate.opsForValue().decrement(key)
                    .onErrorResume(e -> {
                        log.warn("Redis concurrency release error for key {}: {}", key, e.getMessage());
                        releaseLocal(key);
                        return Mono.empty();
                    })
                    .then();
        }

        releaseLocal(key);
        return Mono.empty();
    }

    private boolean acquireLocal(String key, int limit) {
        AtomicInteger counter = localConcurrency.get(key, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();
        if (current > limit) {
            counter.decrementAndGet();
            return false;
        }
        return true;
    }

    private void releaseLocal(String key) {
        AtomicInteger counter = localConcurrency.getIfPresent(key);
        if (counter != null) {
            counter.decrementAndGet();
        }
    }

    public static boolean shouldBypassRateLimit(ServerWebExchange exchange) {
        if (exchange == null || exchange.getRequest() == null) {
            return false;
        }
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        return path != null && RATE_LIMIT_EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }
}
