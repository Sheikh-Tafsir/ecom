package com.example.ecom.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.*;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;

/**
 * Configures resilient defaults for all {@link RestClient} instances used in cross-service
 * communication.
 * <p>
 * Provides:
 * <ul>
 *     <li>Connection timeout (5s) and read timeout (10s)</li>
 *     <li>Automatic retry for read-only (GET) requests on server errors and timeouts (3 attempts)</li>
 *     <li>Standard error handling that converts HTTP errors into appropriate exceptions</li>
 * </ul>
 * <p>
 * Write operations (POST/PUT/DELETE) are NOT retried — non-idempotent writes use
 * compensation logic in the calling service instead.
 */
@Slf4j
@Configuration
public class InternalClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_READ_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 500;

    /**
     * Customizes the auto-configured {@link RestClient.Builder} with timeouts, retry, and error handling.
     * Any bean that injects {@code RestClient.Builder} will get these defaults.
     */
    @Bean
    public RestClientCustomizer internalClientCustomizer() {
        return builder -> builder
                .requestFactory(createRequestFactory())
                .requestInterceptor(readRetryInterceptor())
                .defaultStatusHandler(
                        status -> status.is5xxServerError(),
                        (request, response) -> {
                            String body = new String(response.getBody().readAllBytes());
                            log.error("Internal service error: {} {} → {} {}",
                                    request.getMethod(), request.getURI(),
                                    response.getStatusCode(), body);
                            throw new HttpServerErrorException(
                                    response.getStatusCode(),
                                    "Internal service error: " + response.getStatusCode());
                        }
                );
    }

    /**
     * Retry interceptor that only retries GET (read) requests.
     * Write requests (POST/PUT/DELETE) pass through without retry.
     */
    private ClientHttpRequestInterceptor readRetryInterceptor() {
        return (request, body, execution) -> {
            if (!"GET".equals(request.getMethod().name())) {
                return execution.execute(request, body);
            }

            IOException lastException = null;
            for (int attempt = 1; attempt <= MAX_READ_RETRIES; attempt++) {
                try {
                    ClientHttpResponse response = execution.execute(request, body);
                    if (response.getStatusCode().is5xxServerError() && attempt < MAX_READ_RETRIES) {
                        log.warn("Read retry {}/{} for {} {} → {}",
                                attempt, MAX_READ_RETRIES, request.getMethod(),
                                request.getURI(), response.getStatusCode());
                        response.close();
                        sleep(attempt);
                        continue;
                    }
                    return response;
                } catch (IOException e) {
                    lastException = e;
                    if (attempt < MAX_READ_RETRIES) {
                        log.warn("Read retry {}/{} for {} {} due to: {}",
                                attempt, MAX_READ_RETRIES, request.getMethod(),
                                request.getURI(), e.getMessage());
                        sleep(attempt);
                    }
                }
            }
            throw lastException;
        };
    }

    private static void sleep(int attempt) {
        try {
            Thread.sleep(RETRY_DELAY_MS * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static ClientHttpRequestFactory createRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }
}
