package com.example.ecom.auth.service;

import com.example.ecom.auth.dto.TokenDto;
import com.example.ecom.auth.repository.UserRefreshTokenRepository;
import com.example.ecom.common.enums.UserRefreshTokenStatus;
import com.example.ecom.common.exception.InvalidRefreshTokenException;
import com.example.ecom.common.model.User;
import com.example.ecom.common.model.UserRefreshToken;
import com.example.ecom.common.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static com.example.ecom.common.utils.CacheConstants.CACHE_REVOKED_ACCESS_TOKENS;
import static com.example.ecom.common.utils.CacheConstants.CACHE_ROTATED_TOKENS;
import static com.example.ecom.common.utils.CookieUtils.addCookie;
import static com.example.ecom.common.utils.CookieUtils.getCookieValue;
import static com.example.ecom.common.utils.Utils.isProductionEnvironment;
import static java.util.Objects.isNull;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthTokenService {

    @Value("${spring.profiles.active:dev}")
    private String springProfile;

    @Value("${refresh.token.name:refreshToken}")
    private String refreshTokenName;

    @Value("${refresh.token.validity:604800}")
    private long refreshTokenValidity;

    private final JwtService jwtService;

    private final UserRefreshTokenRepository userRefreshTokenRepository;

    private final CacheManager cacheManager;

    public void addRefreshCookie(HttpServletResponse response, TokenDto authResponse) {
        addCookie(response, refreshTokenName, authResponse.getRefreshToken(), refreshTokenValidity,
                isProductionEnvironment(springProfile));
    }

    @Transactional
    public TokenDto getAuthTokens(User user) {
        return getAuthTokens(user, null);
    }

    @Transactional
    public TokenDto getAuthTokens(User user, Date refreshTokenExpiration) {
        if (user.isNotActive()) {
            throw new ValidationException("User is " + user.getStatus().getValue());
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = isNull(refreshTokenExpiration)
                ? jwtService.generateRefreshToken(user)
                : jwtService.generateRefreshToken(user, refreshTokenExpiration);

        createRefreshTokenRecord(user, refreshToken);

        return TokenDto.builder().accessToken(accessToken).refreshToken(refreshToken).build();
    }

    @Transactional
    public TokenDto refreshAccessToken(HttpServletRequest request) {
        String refreshToken = getCookieValue(request, refreshTokenName);

        if (!isRefreshTokenValid(refreshToken)) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }

        String jti = jwtService.getJtiFromRefreshToken(refreshToken);
        Cache rotatedTokensCache = cacheManager.getCache(CACHE_ROTATED_TOKENS);
        if (rotatedTokensCache != null) {
            TokenDto cachedTokens = rotatedTokensCache.get(jti, TokenDto.class);
            if (cachedTokens != null) {
                log.info("Refresh token JTI: {} was recently rotated. Returning cached tokens.", jti);
                return cachedTokens;
            }
        }

        User user = validateAndRevokeRefreshToken(refreshToken);

        Date refreshTokenExpiration = jwtService.getExpirationFromRefreshToken(refreshToken);
        TokenDto newTokens = getAuthTokens(user, refreshTokenExpiration);

        if (rotatedTokensCache != null) {
            rotatedTokensCache.put(jti, newTokens);
        }

        return newTokens;
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getCookieValue(request, refreshTokenName);

        if (hasText(refreshToken) && jwtService.isRefreshTokenValid(refreshToken)) {
            String jti = jwtService.getJtiFromRefreshToken(refreshToken);
            revokeTokenRecord(jti);
        }

        // Blacklist Access Token in cache
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            try {
                String accessJti = jwtService.getJtiFromAccessToken(accessToken);
                if (accessJti != null) {
                    Cache revokedAccessTokensCache = cacheManager.getCache(CACHE_REVOKED_ACCESS_TOKENS);
                    if (revokedAccessTokensCache != null) {
                        revokedAccessTokensCache.put(accessJti, true);
                    }
                }
            } catch (Exception e) {
                log.warn("Could not extract JTI from access token during logout: {}", e.getMessage());
            }
        }

        addCookie(response, refreshTokenName, null, 0, isProductionEnvironment(springProfile));
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        if (userId != null) {
            userRefreshTokenRepository.revokeAllForUser(userId, UserRefreshTokenStatus.REVOKED);
            log.info("Revoked all active refresh tokens for user: {}", userId);
        }
    }

    // -- Persistence Helpers --

    private void createRefreshTokenRecord(User user, String refreshToken) {
        UserRefreshToken userRefreshToken = new UserRefreshToken();
        userRefreshToken.setUser(user);
        userRefreshToken.setJti(jwtService.getJtiFromRefreshToken(refreshToken));

        userRefreshTokenRepository.save(userRefreshToken);
    }

    private void revokeTokenRecord(String jti) {
        userRefreshTokenRepository.findByJti(jti).ifPresent(token -> {
            token.setStatus(UserRefreshTokenStatus.REVOKED);
        });
    }

    private User validateAndRevokeRefreshToken(String refreshToken) {
        String jti = jwtService.getJtiFromRefreshToken(refreshToken);
        UserRefreshToken userRefreshToken = userRefreshTokenRepository.findDetailsByJti(jti)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

        if (userRefreshToken.isInvalid()) {
            log.warn("Refresh token reuse detected for JTI: {}. Revoking all tokens for user.", jti);
            userRefreshTokenRepository.revokeAllForUser(userRefreshToken.getUser().getId(), UserRefreshTokenStatus.REVOKED);
            throw new InvalidRefreshTokenException("Refresh token has been reused. All active sessions have been invalidated for security.");
        }

        userRefreshToken.setStatus(UserRefreshTokenStatus.REVOKED);

        User user = userRefreshToken.getUser();
        if (isNull(user) || user.isNotActive()) {
            throw new InvalidRefreshTokenException("Refresh token user is invalid");
        }

        return user;
    }

    private boolean isRefreshTokenValid(String token) {
        return hasText(token) && jwtService.isRefreshTokenValid(token);
    }

    @Transactional
    public void deleteRevokedRefreshTokens(int cutoffDays) {
        log.info("Starting cron: Delete Revoked Refresh Tokens (cutoffDays={})", cutoffDays);

        Instant cutoff = Instant.now().minus(Duration.ofDays(cutoffDays));
        int deletedCount = userRefreshTokenRepository.deleteRevoked(UserRefreshTokenStatus.REVOKED, cutoff);

        log.info("Completed cron: Delete Revoked Refresh Tokens (deletedTokens={})", deletedCount);
    }
}
