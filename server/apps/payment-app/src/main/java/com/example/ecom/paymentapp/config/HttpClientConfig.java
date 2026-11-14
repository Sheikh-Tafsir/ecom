package com.example.ecom.paymentapp.config;

import com.example.ecom.common.client.order.OrderClient;
import com.example.ecom.common.client.product.ProductClient;
import com.example.ecom.common.utils.SecurityUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpClientConfig {

    @Value("${services.order.url}")
    private String orderServiceUrl;

    @Value("${services.product.url}")
    private String productServiceUrl;

    @Value("${app.internal.service-key}")
    private String internalKey;

    @Bean
    public OrderClient orderClient(RestClient.Builder builder) {
        RestClient restClient = builder
                .baseUrl(orderServiceUrl)
                .defaultHeader(SecurityUtil.INTERNAL_SERVICE_KEY_HEADER, internalKey)
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(OrderClient.class);
    }

    @Bean
    public ProductClient productClient(RestClient.Builder builder) {
        RestClient restClient = builder
                .baseUrl(productServiceUrl)
                .defaultHeader(SecurityUtil.INTERNAL_SERVICE_KEY_HEADER, internalKey)
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(ProductClient.class);
    }
}
