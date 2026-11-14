package com.example.ecom.common.filter;

import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.common.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.*;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.Callable;

import static com.example.ecom.common.utils.ResponseUtils.error;
import static com.example.ecom.common.utils.SecurityUtil.getUserDetails;

@Slf4j
@RequiredArgsConstructor
public class UserTrafficControlFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return RateLimiterService.shouldBypassRateLimit(request);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String email = getEmailFromContext();

        if (!StringUtils.hasText(email)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. Rate Limit
        if (!rateLimiterService.isUserRateAllowed(email)) {
            log.error("Too many requests from this user: {}", email);
            error(response, HttpStatus.TOO_MANY_REQUESTS, "User rate limit exceeded. Try again later");
            return;
        }

        // 2. Concurrency Throttle
        if (!rateLimiterService.acquireUserConcurrency(email)) {
            log.error("Too many simultaneous requests for this user: {}", email);
            error(response, HttpStatus.TOO_MANY_REQUESTS, "Too many simultaneous requests for this user. Try again later");
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
            if (asyncManager.isConcurrentHandlingStarted()) {
                ConcurrencyReleaseInterceptor interceptor = new ConcurrencyReleaseInterceptor(() -> rateLimiterService.releaseUserConcurrency(email));
                asyncManager.registerDeferredResultInterceptor("userConcurrency", interceptor);
                asyncManager.registerCallableInterceptor("userConcurrency", interceptor);
            } else {
                rateLimiterService.releaseUserConcurrency(email);
            }
        }
    }

    private String getEmailFromContext() {
        CustomUserDetails customUserDetails = getUserDetails();
        return customUserDetails == null ? null : customUserDetails.getEmail();
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
