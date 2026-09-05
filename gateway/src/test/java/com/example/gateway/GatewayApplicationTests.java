package com.example.gateway;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@SpringBootTest(properties = {
    "GATEWAY_PORT=8080",
    "CORS_ALLOWED_ORIGINS=http://localhost:8080",
    "ACCESS_TOKEN_SECRET=test-secret-key-at-least-32-characters-long-for-hmac-sha-256",
    "REDIS_HOST=localhost",
    "REDIS_PORT=6379",
    "REDIS_PASSWORD=",
    "CHAT_SERVICE_URL=http://localhost:3001",
    "MAIN_SERVICE_URL=http://localhost:8081"
})
class GatewayApplicationTests {

    @MockitoBean
    private ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    private ProxyManager<byte[]> proxyManager;

    @Test
    void contextLoads() {
    }

}
