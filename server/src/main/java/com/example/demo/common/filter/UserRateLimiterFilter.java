package com.example.demo.common.filter;

import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.example.demo.common.utils.ResponseUtils.error;
import static com.example.demo.common.utils.SecurityUtil.getUserDetails;

@Slf4j
@RequiredArgsConstructor
public class UserRateLimiterFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String email = getEmailFromContext();

        if (email == null || email.trim().isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!rateLimiterService.isEmailAllowed(email)) {
            reject(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getEmailFromContext() {
        CustomUserDetails customUserDetails = getUserDetails();
        return customUserDetails == null ? null : customUserDetails.getEmail();
    }

    private void reject(HttpServletResponse response) throws IOException {
        error(response, HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Try again later");
    }
}
