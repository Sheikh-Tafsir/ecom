package com.example.demo.common.filter;

import com.example.demo.common.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.example.demo.common.config.SecurityConfig.AUTH_URL;

@Component
@RequiredArgsConstructor
public class RateLimiterFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;

    private final AntPathMatcher pathMatcher;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return pathMatcher.match(AUTH_URL, request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String ip = request.getRemoteAddr();
        String email = getEmailFromContext();

        if (!rateLimiterService.isApiRequestAllowed(email, ip)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too many login attempts. Try again later.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getEmailFromContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return userDetails.getUsername();
        }

        return null;
    }
}
