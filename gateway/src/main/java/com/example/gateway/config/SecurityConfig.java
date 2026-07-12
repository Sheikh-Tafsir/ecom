package com.example.gateway.config;

import com.example.gateway.filter.AuthenticationFilter;
import com.example.gateway.filter.IpRateLimiterFilter;
import com.example.gateway.filter.UserRateLimiterFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    public static final Set<String> PUBLIC_URLS = Set.of(
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-resources/**",
            "/webjars/**",
            "/actuator",
            "/actuator/**",
            "/socket.io/**"
    );

    @Value("${cors.allowed.origins}")
    private String allowedOrigins;

    private final com.example.gateway.service.JwtService jwtService;
    private final com.example.gateway.service.RateLimiterService rateLimiterService;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        AuthenticationFilter authenticationFilter = new AuthenticationFilter(jwtService);
        IpRateLimiterFilter ipRateLimiterFilter = new IpRateLimiterFilter(rateLimiterService);
        UserRateLimiterFilter userRateLimiterFilter = new UserRateLimiterFilter(rateLimiterService);

        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((exchange, e) -> {
                            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                )
                .addFilterBefore(ipRateLimiterFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .addFilterBefore(authenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .addFilterAfter(userRateLimiterFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(PUBLIC_URLS.toArray(new String[0])).permitAll()
                        .pathMatchers("/auth/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/products", "/products/*").permitAll()
                        .pathMatchers(HttpMethod.GET, "/categories", "/categories/*").permitAll()
                        .pathMatchers("/payment/callback").permitAll()
                        .anyExchange().authenticated()
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList()
        );
        config.setAllowedHeaders(List.of(
                "Content-Type",
                "Authorization",
                "Idempotency-Key",
                "X-XSRF-TOKEN"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
