package com.example.ecom.common.config;

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
import java.util.concurrent.TimeUnit;

import static com.example.ecom.common.utils.CacheConstants.*;
import static org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair.fromSerializer;

@Configuration
@EnableCaching
@Slf4j
public class CacheConfig implements CachingConfigurer {

    @Value("${app.cache.ttl.default-minutes:5}")
    public int defaultTtl;

    @Value("${app.cache.ttl.idempotency-minutes:60}")
    protected int idempotencyTtl;

    @Value("${app.cache.ttl.otp-minutes:5}")
    protected int otpTtl;

    @Value("${app.cache.ttl.sse-ticket-minutes:1}")
    protected int sseTicketTtl;

    @Value("${app.cache.ttl.cms-minutes:10}")
    protected int cmsTtl;

    @Value("${app.cache.ttl.token-rotation-seconds:60}")
    protected int tokenRotationTtl;

    /**
     * Principal Engineering Practice: Resilience.
     * If Redis is down or data is corrupt, log the error and fall back to the Database.
     * This prevents 500 errors for the end user.
     */
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
        public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, RedisSerializer<Object> redisSerializer) {
            RedisCacheConfiguration defaultConfig = buildConfig(defaultTtl, redisSerializer);

            Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

            cacheConfigurations.put(CACHE_IDEMPOTENCY, buildConfig(idempotencyTtl, redisSerializer));
            cacheConfigurations.put(CACHE_OTPS, buildConfig(otpTtl, redisSerializer));
            cacheConfigurations.put(CACHE_SSE_TICKETS, buildConfig(sseTicketTtl, redisSerializer));

            RedisCacheConfiguration cmsConfig = buildConfig(cmsTtl, redisSerializer);
            cacheConfigurations.put(CACHE_BANNERS, cmsConfig);
            cacheConfigurations.put(CACHE_FAQS, cmsConfig);
            cacheConfigurations.put(CACHE_BLOGS, cmsConfig);
            cacheConfigurations.put(CACHE_CATEGORIES, cmsConfig);

            cacheConfigurations.put(CACHE_PRODUCTS, buildConfig(15, redisSerializer));
            cacheConfigurations.put(CACHE_PRODUCTS_EDIT, buildConfig(15, redisSerializer));
            cacheConfigurations.put(CACHE_ROLES, buildConfig(30, redisSerializer));
            cacheConfigurations.put(CACHE_ROLE, buildConfig(30, redisSerializer));
            cacheConfigurations.put(CACHE_USER, buildConfig(10, redisSerializer));
            cacheConfigurations.put(CACHE_PROFILE, buildConfig(10, redisSerializer));

            cacheConfigurations.put(CACHE_ROTATED_TOKENS, buildConfig(1, redisSerializer)); // 1 minute
            cacheConfigurations.put(CACHE_REVOKED_ACCESS_TOKENS, buildConfig(60, redisSerializer)); // 1 hour

            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(defaultConfig)
                    .withInitialCacheConfigurations(cacheConfigurations)
                    .build();
        }

        private RedisCacheConfiguration buildConfig(int ttlMinutes, RedisSerializer<Object> serializer) {
            return RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(ttlMinutes))
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
            CaffeineCacheManager cacheManager = new CaffeineCacheManager();

            // Standard CMS & Catalog Caches
            cacheManager.registerCustomCache(CACHE_BANNERS, buildCaffeineCache(cmsTtl));
            cacheManager.registerCustomCache(CACHE_FAQS, buildCaffeineCache(cmsTtl));
            cacheManager.registerCustomCache(CACHE_BLOGS, buildCaffeineCache(cmsTtl));
            cacheManager.registerCustomCache(CACHE_CATEGORIES, buildCaffeineCache(cmsTtl));
            cacheManager.registerCustomCache(CACHE_PRODUCTS, buildCaffeineCache(15));
            cacheManager.registerCustomCache(CACHE_PRODUCTS_EDIT, buildCaffeineCache(15));
            cacheManager.registerCustomCache(CACHE_ROLES, buildCaffeineCache(30));
            cacheManager.registerCustomCache(CACHE_ROLE, buildCaffeineCache(30));
            cacheManager.registerCustomCache(CACHE_USER, buildCaffeineCache(10));
            cacheManager.registerCustomCache(CACHE_PROFILE, buildCaffeineCache(10));

            // System Caches
            cacheManager.registerCustomCache(CACHE_OTPS, buildCaffeineCache(otpTtl));
            cacheManager.registerCustomCache(CACHE_IDEMPOTENCY, buildCaffeineCache(idempotencyTtl));
            cacheManager.registerCustomCache(CACHE_SSE_TICKETS, buildCaffeineCache(sseTicketTtl));

            cacheManager.registerCustomCache(CACHE_ROTATED_TOKENS, buildCaffeineCache(1)); // 1 minute
            cacheManager.registerCustomCache(CACHE_REVOKED_ACCESS_TOKENS, buildCaffeineCache(60)); // 1 hour

            // Default configuration for any other caches
            cacheManager.setCaffeine(Caffeine.newBuilder()
                    .expireAfterWrite(defaultTtl, TimeUnit.MINUTES));

            return cacheManager;
        }

        private com.github.benmanes.caffeine.cache.Cache<Object, Object> buildCaffeineCache(int ttlMinutes) {
            return Caffeine.newBuilder()
                    .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                    .build();
        }
    }
}
