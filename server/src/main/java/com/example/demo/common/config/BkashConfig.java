package com.example.demo.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "bkash")
public class BkashConfig {
    private String baseUrl;
    private String appKey;
    private String appSecret;
    private String username;
    private String password;
    private String callbackUrl;
    private String frontendSuccessUrl;
    private String frontendFailUrl;
}