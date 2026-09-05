package com.example.gateway.config;

import com.example.gateway.filter.AuthenticationFilter;
import com.example.gateway.filter.UserRateLimiterFilter;
import com.example.gateway.service.JwtService;
import com.example.gateway.service.RateLimiterService;
import com.example.gateway.util.ResponseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;
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
            "/socket.io/**",
            "/notifications/subscribe"
    );

    @Value("${cors.allowed.origins}")
    private String allowedOrigins;

    @Value("${app.cache.revoked-tokens-prefix}")
    private String revokedTokensPrefix;

    private final JwtService jwtService;

    private final RateLimiterService rateLimiterService;

    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    @Bean
    public AuthenticationFilter authenticationFilter() {
        return new AuthenticationFilter(jwtService, reactiveStringRedisTemplate, revokedTokensPrefix);
    }

    @Bean
    public UserRateLimiterFilter userRateLimiterFilter() {
        return new UserRateLimiterFilter(rateLimiterService);
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.mode(XFrameOptionsServerHttpHeadersWriter.Mode.DENY))
                        .contentTypeOptions(Customizer.withDefaults())
                        .xssProtection(Customizer.withDefaults())
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((exchange, e) ->
                                ResponseUtils.error(exchange, HttpStatus.UNAUTHORIZED, "Full authentication is required to access this resource")
                        )
                )
                .addFilterBefore(authenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .addFilterAfter(userRateLimiterFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(PUBLIC_URLS.toArray(new String[0])).permitAll()
                        .pathMatchers("/auth/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/products", "/products/*").permitAll()
                        .pathMatchers(HttpMethod.GET, "/categories", "/categories/*").permitAll()
                        .pathMatchers(HttpMethod.GET, "/banners", "/banners/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/blogs", "/blogs/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/faqs", "/faqs/**").permitAll()
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
