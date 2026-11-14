package com.example.ecom.orderapp.config;

import com.example.ecom.common.client.product.ProductClient;
import com.example.ecom.common.client.stock.StockClient;
import com.example.ecom.common.utils.SecurityUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpClientConfig {

    @Value("${services.product.url}")
    private String productServiceUrl;

    @Value("${app.internal.service-key}")
    private String internalKey;

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

    @Bean
    public StockClient stockClient(RestClient.Builder builder) {
        RestClient restClient = builder
                .baseUrl(productServiceUrl)
                .defaultHeader(SecurityUtil.INTERNAL_SERVICE_KEY_HEADER, internalKey)
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(StockClient.class);
    }
}
