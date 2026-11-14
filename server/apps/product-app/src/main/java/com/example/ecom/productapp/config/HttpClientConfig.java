package com.example.ecom.productapp.config;

import com.example.ecom.common.client.user.UserClient;
import com.example.ecom.common.utils.SecurityUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpClientConfig {

    @Value("${services.user.url}")
    private String userServiceUrl;

    @Value("${app.internal.service-key}")
    private String internalKey;

    @Bean
    public UserClient userClient(RestClient.Builder builder) {
        RestClient restClient = builder
                .baseUrl(userServiceUrl)
                .defaultHeader(SecurityUtil.INTERNAL_SERVICE_KEY_HEADER, internalKey)
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(UserClient.class);
    }
}
