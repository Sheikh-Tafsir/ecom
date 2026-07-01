package com.example.gateway.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class Bucket4jConfig {

    @Bean
    public ProxyManager<byte[]> proxyManager(RedisClient redisClient) {
        ClientSideConfig clientSideConfig = ClientSideConfig.getDefault()
                .withExpirationAfterWriteStrategy(ExpirationAfterWriteStrategy.fixedTimeToLive(Duration.ofDays(1)));

        return LettuceBasedProxyManager.builderFor(redisClient)
                .withClientSideConfig(clientSideConfig)
                .build();
    }
}
