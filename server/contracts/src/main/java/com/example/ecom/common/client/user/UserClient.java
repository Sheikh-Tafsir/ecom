package com.example.ecom.common.client.user;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.UUID;

/**
 * Cross-domain contract for user queries needed by other domains (e.g., Review).
 * <p>
 * Monolith: implemented by {@code UserClientAdapter} in the user domain.
 * Microservice: Spring generates an HTTP client via {@code @HttpExchange}.
 */
@HttpExchange("/internal/users")
public interface UserClient {

    @GetExchange("/{id}")
    UserBasicDto findUserById(@PathVariable UUID id);
}
