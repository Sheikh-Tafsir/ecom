package com.example.ecom.common.config;

import com.example.ecom.common.cache.ResilientCacheManager;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.example.ecom.common.utils.CacheConstants.*;
import static org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair.fromSerializer;

@Configuration
@EnableCaching
@Slf4j
public class CacheConfig implements CachingConfigurer {

    @Value("${app.cache.ttl.default-minutes}")
    public int defaultTtl;

    @Value("${app.cache.ttl.idempotency-minutes}")
    protected int idempotencyTtl;

    @Value("${app.cache.ttl.otp-minutes}")
    protected int otpTtl;

    @Value("${app.cache.ttl.cms-minutes}")
    protected int cmsTtl;

    @Value("${jwt.access-token.expiration}")
    protected int accessTokenValidity;

    /**
     * Caches that MUST NOT fall back to in-memory on Redis failure.
     *
     * @see <a href="docs/decisions/006-resilience-and-fallbacks.md">ADR 006</a>
     */
    private static final Set<String> FAIL_CLOSED_CACHES = Set.of(CACHE_IDEMPOTENCY);

    /** Single source of truth for every cache name → TTL mapping. */
    protected Map<String, Duration> buildTtlMap() {
        int revokedTokensTtlMinutes = Math.max(1, accessTokenValidity / 60);

        Map<String, Duration> map = new HashMap<>();

        // Entity & Catalog
        map.put(CACHE_USER, Duration.ofMinutes(10));
        map.put(CACHE_PROFILE, Duration.ofMinutes(10));
        map.put(CACHE_PRODUCTS, Duration.ofMinutes(15));
        map.put(CACHE_PRODUCTS_EDIT, Duration.ofMinutes(15));
        map.put(CACHE_ROLES, Duration.ofMinutes(30));
        map.put(CACHE_ROLE, Duration.ofMinutes(30));

        // CMS
        map.put(CACHE_BANNERS, Duration.ofMinutes(cmsTtl));
        map.put(CACHE_FAQS, Duration.ofMinutes(cmsTtl));
        map.put(CACHE_BLOGS, Duration.ofMinutes(cmsTtl));
        map.put(CACHE_CATEGORIES, Duration.ofMinutes(cmsTtl));

        // Security & System
        map.put(CACHE_IDEMPOTENCY, Duration.ofMinutes(idempotencyTtl));
        map.put(CACHE_OTPS, Duration.ofMinutes(otpTtl));
        map.put(CACHE_ROTATED_TOKENS, Duration.ofMinutes(1));
        map.put(CACHE_REVOKED_ACCESS_TOKENS, Duration.ofMinutes(revokedTokensTtlMinutes));

        // Payment
        map.put(CACHE_BKASH_ID_TOKEN, Duration.ofMinutes(55));
        map.put(CACHE_BKASH_REFRESH_TOKEN, Duration.ofDays(27));

        return map;
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.error("Cache GET error for key {} in cache {}: {}", key, cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.error("Cache PUT error for key {} in cache {}: {}", key, cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.error("Cache EVICT error for key {} in cache {}: {}", key, cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.error("Cache CLEAR error for cache {}: {}", cache.getName(), exception.getMessage());
            }
        };
    }

    @Configuration
    @ConditionalOnExpression("'${app.cache.strategy:redis}' == 'redis'")
    public class RedisCacheSetup {

        @Bean
        public CacheManager cacheManager(RedisConnectionFactory connectionFactory, RedisSerializer<Object> redisSerializer) {
            Map<String, Duration> ttlMap = buildTtlMap();

            RedisCacheManager redisManager = buildRedisManager(connectionFactory, redisSerializer, ttlMap);
            redisManager.afterPropertiesSet();

            return new ResilientCacheManager(redisManager, ttlMap,
                    Duration.ofMinutes(defaultTtl), FAIL_CLOSED_CACHES);
        }

        private RedisCacheManager buildRedisManager(RedisConnectionFactory connectionFactory,
                                                    RedisSerializer<Object> redisSerializer,
                                                    Map<String, Duration> ttlMap) {
            Map<String, RedisCacheConfiguration> configs = new HashMap<>();
            ttlMap.forEach((name, ttl) -> configs.put(name, buildRedisConfig(ttl, redisSerializer)));

            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(buildRedisConfig(Duration.ofMinutes(defaultTtl), redisSerializer))
                    .withInitialCacheConfigurations(configs)
                    .build();
        }

        private RedisCacheConfiguration buildRedisConfig(Duration ttl, RedisSerializer<Object> serializer) {
            return RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(ttl)
                    .computePrefixWith(cacheName -> CACHE_VERSION + ":" + cacheName + "::")
                    .serializeKeysWith(fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(fromSerializer(serializer));
        }
    }

    @Configuration
    @ConditionalOnExpression("'${app.cache.strategy:redis}' == 'local'")
    public class LocalCacheSetup {

        @Bean
        public CacheManager cacheManager() {
            Map<String, Duration> ttlMap = buildTtlMap();

            CaffeineCacheManager cacheManager = new CaffeineCacheManager();
            ttlMap.forEach((name, ttl) -> cacheManager.registerCustomCache(name, buildCaffeineCache(ttl)));

            // Default for any other caches
            cacheManager.setCaffeine(Caffeine.newBuilder()
                    .expireAfterWrite(defaultTtl, TimeUnit.MINUTES));

            return cacheManager;
        }

        private com.github.benmanes.caffeine.cache.Cache<Object, Object> buildCaffeineCache(Duration ttl) {
            return Caffeine.newBuilder()
                    .expireAfterWrite(ttl)
                    .build();
        }
    }
}
