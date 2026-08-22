package com.example.ecom.auth.service;

import com.example.ecom.auth.repository.UserRefreshTokenRepository;
import com.example.ecom.common.enums.UserRefreshTokenStatus;
import com.example.ecom.common.exception.InvalidRefreshTokenException;
import com.example.ecom.common.model.User;
import com.example.ecom.common.model.UserRefreshToken;
import com.example.ecom.common.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static java.util.Objects.isNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRefreshTokenService {

    private final UserRefreshTokenRepository userRefreshTokenRepository;

    private final JwtService jwtService;

    @Transactional
    public void create(User user, String refreshToken) {
        UserRefreshToken userRefreshToken = new UserRefreshToken();
        userRefreshToken.setUser(user);
        userRefreshToken.setJti(jwtService.getJtiFromRefreshToken(refreshToken));

        userRefreshTokenRepository.save(userRefreshToken);
    }

    @Transactional
    public void revoke(String jti) {
        UserRefreshToken token = findByJti(jti);

        if (token != null) {
            token.setStatus(UserRefreshTokenStatus.REVOKED);
        }
    }

    @Transactional
    public User validate(String refreshToken) {
        String jti = jwtService.getJtiFromRefreshToken(refreshToken);
        UserRefreshToken userRefreshToken = findByJti(jti);

        if (userRefreshToken == null) {
            throw new InvalidRefreshTokenException("Refresh token not found");
        }

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

    @Transactional
    public void deleteRevoked() {
        java.time.Instant cutoff = java.time.Instant.now().minus(24, java.time.temporal.ChronoUnit.HOURS);
        int deletedCount = userRefreshTokenRepository.deleteRevoked(UserRefreshTokenStatus.REVOKED, cutoff);

        log.info("Deleted {} revoked refresh tokens", deletedCount);
    }

    // --helpers --
    private UserRefreshToken findByJti(String jti) {
        return userRefreshTokenRepository.findByJti(jti).orElse(null);
    }
}
