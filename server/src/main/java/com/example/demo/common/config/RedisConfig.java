package com.example.demo.common.config;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import static org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair.fromSerializer;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public JedisPool jedisPool(RedisProperties properties) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setJmxEnabled(false);
        return new JedisPool(poolConfig, properties.getHost(), properties.getPort(), 2000, properties.getPassword());
    }

//    @Bean
//    public RedisConnectionFactory redisConnectionFactory(RedisProperties redisProperties) {
//        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
//                .useSsl()
//                .build();
//
//        var config = new org.springframework.data.redis.connection.RedisStandaloneConfiguration();
//        config.setHostName(redisProperties.getHost());
//        config.setPort(redisProperties.getPort());
//        config.setUsername(redisProperties.getUsername());
//        config.setPassword(redisProperties.getPassword());
//
//        return new LettuceConnectionFactory(config, clientConfig);
//    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(RedisSerializer.json());
        template.setHashValueSerializer(RedisSerializer.json());

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(fromSerializer(RedisSerializer.json()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
