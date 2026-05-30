package com.example.demo.common.filter;

import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.example.demo.common.utils.ResponseUtils.filterError;
import static com.example.demo.common.utils.SecurityUtil.getUserDetails;

@Component
@RequiredArgsConstructor
public class RateLimiterFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String ip = request.getRemoteAddr();
        String email = getEmailFromContext();

        if (!rateLimiterService.isApiRequestAllowed(email, ip)) {
            filterError(response, HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Try again later");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getEmailFromContext() {
        CustomUserDetails customUserDetails = getUserDetails();
        return customUserDetails == null ? null : customUserDetails.getEmail();
    }
}
