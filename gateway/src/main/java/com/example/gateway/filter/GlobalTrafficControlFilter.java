package com.example.gateway.filter;

import com.example.gateway.service.RateLimiterService;
import com.example.gateway.util.ResponseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class GlobalTrafficControlFilter implements WebFilter {

    private final RateLimiterService rateLimiterService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (RateLimiterService.shouldBypassRateLimit(exchange)) {
            return chain.filter(exchange);
        }

        // 1. Check Global Rate Limit
        return rateLimiterService.isGlobalRateAllowed()
                .flatMap(rateAllowed -> {
                    if (!rateAllowed) {
                        log.error("Too many global requests");
                        return ResponseUtils.error(exchange, HttpStatus.TOO_MANY_REQUESTS, "Too many global requests, please try again later");
                    }

                    // 2. Concurrency Throttle (Bulkhead) — use Mono.usingWhen for proper lifecycle
                    return rateLimiterService.acquireGlobalConcurrency()
                            .flatMap(concurrencyAllowed -> {
                                if (!concurrencyAllowed) {
                                    log.error("Global concurrency limit exceeded");
                                    return ResponseUtils.error(exchange, HttpStatus.SERVICE_UNAVAILABLE, "Global concurrency limit exceeded, please try again later");
                                }

                                return Mono.usingWhen(
                                        Mono.just(true),
                                        acquired -> chain.filter(exchange),
                                        acquired -> rateLimiterService.releaseGlobalConcurrency(),
                                        (acquired, err) -> rateLimiterService.releaseGlobalConcurrency(),
                                        acquired -> rateLimiterService.releaseGlobalConcurrency()
                                );
                            });
                });
    }
}
