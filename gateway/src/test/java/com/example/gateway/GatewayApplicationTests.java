package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "CORS_ALLOWED_ORIGINS=http://localhost:8080",
    "ACCESS_TOKEN_SECRET=test-secret-key-at-least-32-characters-long-for-hmac-sha-256"
})
class GatewayApplicationTests {

    @Test
    void contextLoads() {
    }

}
