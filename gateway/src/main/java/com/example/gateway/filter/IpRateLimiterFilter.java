package com.example.gateway.filter;

import com.example.gateway.service.RateLimiterService;
import com.example.gateway.util.ResponseUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class IpRateLimiterFilter implements WebFilter {

    private static final String AUTH_PATH = "/auth";

    private final RateLimiterService rateLimiterService;

    private final Set<String> trustedProxies;

    public IpRateLimiterFilter(
            RateLimiterService rateLimiterService,
            @Value("${rate-limit.trusted-proxies:}") String trustedProxiesConfig
    ) {
        this.rateLimiterService = rateLimiterService;
        if (trustedProxiesConfig != null && !trustedProxiesConfig.isBlank()) {
            this.trustedProxies = Arrays.stream(trustedProxiesConfig.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());
        } else {
            this.trustedProxies = Set.of();
        }
        log.info("Trusted proxies configured: {}", this.trustedProxies);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        if (RateLimiterService.shouldBypassRateLimit(exchange)) {
            return chain.filter(exchange);
        }

        String ip = extractIp(exchange.getRequest());

        if (ip == null || ip.isBlank()) {
            log.warn("Missing client IP");
            return ResponseUtils.error(exchange, HttpStatus.BAD_REQUEST, "Missing client IP address");
        }

        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        boolean isAuthPath = path != null && path.startsWith(AUTH_PATH);

        // 1. IP Rate Limit
        return rateLimiterService.isIpRateAllowed(ip, isAuthPath)
                .flatMap(rateAllowed -> {
                    if (!rateAllowed) {
                        log.error("Too many requests from IP: {}", ip);
                        return ResponseUtils.error(exchange, HttpStatus.TOO_MANY_REQUESTS, "Too many requests from your IP, please try again later");
                    }

                    // 2. IP Concurrency Throttle
                    return rateLimiterService.acquireIpConcurrency(ip, isAuthPath)
                            .flatMap(concurrencyAllowed -> {
                                if (!concurrencyAllowed) {
                                    log.error("Too many simultaneous connections from IP: {}", ip);
                                    return ResponseUtils.error(exchange, HttpStatus.TOO_MANY_REQUESTS, "Too many simultaneous connections from your IP");
                                }

                                return Mono.usingWhen(
                                        Mono.just(true),
                                        acquired -> chain.filter(exchange),
                                        acquired -> rateLimiterService.releaseIpConcurrency(ip, isAuthPath),
                                        (acquired, err) -> rateLimiterService.releaseIpConcurrency(ip, isAuthPath),
                                        acquired -> rateLimiterService.releaseIpConcurrency(ip, isAuthPath)
                                );
                            });
                });
    }

    public String extractIp(ServerHttpRequest request) {
        // Only trust X-Forwarded-For when coming from an explicitly configured trusted proxy
        if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
            InetAddress remoteAddress = request.getRemoteAddress().getAddress();
            if (isTrustedProxy(remoteAddress)) {
                String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    return forwarded.split(",")[0].trim();
                }

                String realIp = request.getHeaders().getFirst("X-Real-IP");
                if (realIp != null && !realIp.isBlank()) {
                    return realIp.trim();
                }
            }
            return remoteAddress.getHostAddress();
        }

        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        return null;
    }

    private boolean isTrustedProxy(InetAddress address) {
        if (address == null) return false;
        // Always trust loopback (127.0.0.1, ::1)
        if (address.isLoopbackAddress()) return true;
        // Only trust explicitly configured proxy IPs — not the entire private address space
        return trustedProxies.contains(address.getHostAddress());
    }
}
