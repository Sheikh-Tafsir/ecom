package com.example.ecom.common.service;

import com.example.ecom.common.config.CacheConfig;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.ecom.common.utils.CacheConstants.*;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.standalone", havingValue = "true")
@RequiredArgsConstructor
public class RateLimiterService {

    public static final Set<String> RATE_LIMIT_EXCLUDED_PATHS = Set.of(
            "/notifications/subscribe"
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

    @Value("${rate-limit.otp.rate:1}")
    private int otpRateLimit;

    @Value("${rate-limit.otp.window-seconds:60}")
    private int otpWindowSeconds;

    @Value("${rate-limit.window-seconds:60}")
    private int windowSeconds;

    private final Optional<ProxyManager<byte[]>> proxyManager;

    private final Optional<StringRedisTemplate> redisTemplate;

    private final CacheConfig cacheConfig;

    private final Map<Integer, BucketConfiguration> configCache = new ConcurrentHashMap<>();

    // Fallbacks for non-Redis mode
    private final Cache<String, Bucket> localBuckets = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(10000)
            .build();

    private final Cache<String, AtomicInteger> localConcurrency = Caffeine.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .maximumSize(5000)
            .build();

    // --- Rate Limiting (Frequency) ---
    public boolean isGlobalRateAllowed() {
        return isRateAllowed(RATE_LIMIT_PREFIX + GLOBAL_SCOPE, globalRateLimit);
    }

    public boolean isIpRateAllowed(String ip, boolean isAuth) {
        String key = isAuth ? RATE_LIMIT_PREFIX + AUTH_SCOPE_PREFIX + ip : RATE_LIMIT_PREFIX + IP_SCOPE_PREFIX + ip;
        int limit = isAuth ? authIpRateLimit : ipRateLimit;
        return isRateAllowed(key, limit);
    }

    public boolean isUserRateAllowed(String email) {
        return isRateAllowed(RATE_LIMIT_PREFIX + USER_SCOPE_PREFIX + email, userRateLimit);
    }

    public boolean isOtpRateAllowed(String identifier) {
        BucketConfiguration config = configCache.computeIfAbsent(-1, l -> BucketConfiguration.builder()
                .addLimit(limitBuilder -> limitBuilder
                        .capacity(otpRateLimit)
                        .refillIntervally(otpRateLimit, Duration.ofSeconds(otpWindowSeconds)))
                .build());

        String key = RATE_LIMIT_PREFIX + OTP_SCOPE_PREFIX + identifier;
        return tryConsume(key, config);
    }

    private boolean isRateAllowed(String key, int limit) {
        BucketConfiguration config = configCache.computeIfAbsent(limit, l -> BucketConfiguration.builder()
                .addLimit(limitBuilder -> limitBuilder
                        .capacity(l)
                        .refillIntervally(l, Duration.ofSeconds(windowSeconds)))
                .build());

        return tryConsume(key, config);
    }

    private boolean tryConsume(String key, BucketConfiguration config) {
        if (proxyManager.isPresent()) {
            Bucket bucket = proxyManager.get().builder().build(key.getBytes(), config);
            return bucket.tryConsume(1);
        }

        // Local Fallback
        Bucket bucket = localBuckets.get(key, k -> Bucket.builder().addLimit(config.getBandwidths()[0]).build());
        return bucket.tryConsume(1);
    }

    // --- Concurrency Throttling (Bulkhead) ---

    public boolean acquireGlobalConcurrency() {
        return acquire(CONCURRENCY_LIMIT_PREFIX + GLOBAL_SCOPE, globalConcurrencyLimit);
    }

    public void releaseGlobalConcurrency() {
        release(CONCURRENCY_LIMIT_PREFIX + GLOBAL_SCOPE);
    }

    public boolean acquireIpConcurrency(String ip, boolean isAuth) {
        String key = isAuth ? CONCURRENCY_LIMIT_PREFIX + AUTH_SCOPE_PREFIX + ip : CONCURRENCY_LIMIT_PREFIX + IP_SCOPE_PREFIX + ip;
        int limit = isAuth ? authIpConcurrencyLimit : ipConcurrencyLimit;
        return acquire(key, limit);
    }

    public void releaseIpConcurrency(String ip, boolean isAuth) {
        String key = isAuth ? CONCURRENCY_LIMIT_PREFIX + AUTH_SCOPE_PREFIX + ip : CONCURRENCY_LIMIT_PREFIX + IP_SCOPE_PREFIX + ip;
        release(key);
    }

    public boolean acquireUserConcurrency(String email) {
        return acquire(CONCURRENCY_LIMIT_PREFIX + USER_SCOPE_PREFIX + email, userConcurrencyLimit);
    }

    public void releaseUserConcurrency(String email) {
        release(CONCURRENCY_LIMIT_PREFIX + USER_SCOPE_PREFIX + email);
    }

    public static boolean shouldBypassRateLimit(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String uri = request.getRequestURI();
        return uri != null && RATE_LIMIT_EXCLUDED_PATHS.stream().anyMatch(uri::startsWith);
    }

    private boolean acquire(String key, int limit) {
        if (redisTemplate.isPresent()) {
            Long current = redisTemplate.get().opsForValue().increment(key);
            if (current == null) return false;

            if (current > limit) {
                redisTemplate.get().opsForValue().decrement(key);
                return false;
            }

            if (current == 1) {
                redisTemplate.get().expire(key, Duration.ofMinutes(cacheConfig.defaultTtl));
            }

            return true;
        }

        // Local Fallback
        AtomicInteger counter = localConcurrency.get(key, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();
        if (current > limit) {
            counter.decrementAndGet();
            return false;
        }
        return true;
    }

    private void release(String key) {
        if (redisTemplate.isPresent()) {
            redisTemplate.get().opsForValue().decrement(key);
            return;
        }

        // Local Fallback
        AtomicInteger counter = localConcurrency.getIfPresent(key);
        if (counter != null) {
            counter.decrementAndGet();
        }
    }
}
