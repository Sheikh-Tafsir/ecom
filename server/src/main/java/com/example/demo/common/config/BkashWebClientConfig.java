package com.example.demo.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class BkashWebClientConfig {

    @Bean
    public WebClient bkashWebClient(BkashConfig config) {
        return WebClient.builder()
                .baseUrl(config.getBaseUrl()) // ← no trailing /
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("username", config.getUsername())      // ← bKash needs these
                .defaultHeader("password", config.getPassword())      // ← as default headers
                .codecs(c -> c.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }
}
