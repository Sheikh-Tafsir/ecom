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
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.*;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.Callable;

import static com.example.ecom.common.utils.ResponseUtils.error;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@ConditionalOnProperty(name = "app.standalone", havingValue = "true")
@RequiredArgsConstructor
public class GlobalTrafficControlFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return RateLimiterService.shouldBypassRateLimit(request);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 1. Rate Limit
        if (!rateLimiterService.isGlobalRateAllowed()) {
            log.error("Too many requests");
            error(response, HttpStatus.TOO_MANY_REQUESTS, "Server is under high load. Try again later");
            return;
        }

        // 2. Concurrency Throttle
        if (!rateLimiterService.acquireGlobalConcurrency()) {
            log.error("Too many simultaneous connections");
            error(response, HttpStatus.SERVICE_UNAVAILABLE, "Server is at maximum capacity. Try again later");
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
            if (asyncManager.isConcurrentHandlingStarted()) {
                ConcurrencyReleaseInterceptor interceptor = new ConcurrencyReleaseInterceptor(rateLimiterService::releaseGlobalConcurrency);
                asyncManager.registerDeferredResultInterceptor("globalConcurrency", interceptor);
                asyncManager.registerCallableInterceptor("globalConcurrency", interceptor);
            } else {
                rateLimiterService.releaseGlobalConcurrency();
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
