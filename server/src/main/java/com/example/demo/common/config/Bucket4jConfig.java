package com.example.demo.common.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.jedis.cas.JedisBasedProxyManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;

@Configuration
public class Bucket4jConfig {

    @Bean
    public ProxyManager<byte[]> proxyManager(JedisPool jedisPool) {
        return JedisBasedProxyManager.builderFor(jedisPool).build();
    }
}
