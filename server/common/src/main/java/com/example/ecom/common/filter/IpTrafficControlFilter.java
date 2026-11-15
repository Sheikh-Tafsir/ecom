package com.example.ecom.common.filter;

import com.example.ecom.common.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.*;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.Callable;

import static com.example.ecom.common.utils.RequestUtil.getClientIp;
import static com.example.ecom.common.utils.ResponseUtils.error;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@ConditionalOnProperty(name = "app.standalone", havingValue = "true")
@RequiredArgsConstructor
public class IpTrafficControlFilter extends OncePerRequestFilter {

    private static final String AUTH_PATH = "/auth";

    private final RateLimiterService rateLimiterService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return RateLimiterService.shouldBypassRateLimit(request);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String ip = getClientIp(request);

        if (!StringUtils.hasText(ip)) {
            log.warn("Missing client IP");
            error(response, HttpStatus.BAD_REQUEST, "IP address required");
            return;
        }

        boolean isAuthPath = request.getRequestURI().startsWith(AUTH_PATH);

        // 1. Rate Limit
        if (!rateLimiterService.isIpRateAllowed(ip, isAuthPath)) {
            log.error("Too many requests from this IP: {}", ip);
            error(response, HttpStatus.TOO_MANY_REQUESTS, "Too many requests from this device. Try again later");
            return;
        }

        // 2. Concurrency Throttle
        if (!rateLimiterService.acquireIpConcurrency(ip, isAuthPath)) {
            log.error("Too many simultaneous connections from this IP: {}", ip);
            error(response, HttpStatus.TOO_MANY_REQUESTS, "Too many simultaneous connections from this device. Try again later");
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
            if (asyncManager.isConcurrentHandlingStarted()) {
                ConcurrencyReleaseInterceptor interceptor = new ConcurrencyReleaseInterceptor(() -> rateLimiterService.releaseIpConcurrency(ip, isAuthPath));
                asyncManager.registerDeferredResultInterceptor("ipConcurrency", interceptor);
                asyncManager.registerCallableInterceptor("ipConcurrency", interceptor);
            } else {
                rateLimiterService.releaseIpConcurrency(ip, isAuthPath);
            }
        }
    }

    @RequiredArgsConstructor
    private static class ConcurrencyReleaseInterceptor implements CallableProcessingInterceptor, DeferredResultProcessingInterceptor {
        private final Runnable releaseAction;

        @Override
        public <T> void afterCompletion(@NonNull NativeWebRequest request, @NonNull Callable<T> task) {
            releaseAction.run();
        }

        @Override
        public <T> void afterCompletion(@NonNull NativeWebRequest request, @NonNull DeferredResult<T> deferredResult) {
            releaseAction.run();
        }
    }
}
