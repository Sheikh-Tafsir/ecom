package com.example.demo.auth.service;

import com.example.demo.auth.repository.UserRefreshTokenRepository;
import com.example.demo.common.enums.UserRefreshTokenStatus;
import com.example.demo.common.exception.InvalidRefreshTokenException;
import com.example.demo.common.model.User;
import com.example.demo.common.model.UserRefreshToken;
import com.example.demo.common.service.JwtService;
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

    public User validate(String refreshToken) {
        String jti = jwtService.getJtiFromRefreshToken(refreshToken);
        UserRefreshToken userRefreshToken = findByJti(jti);

        if (userRefreshToken == null || userRefreshToken.isInvalid()) {
            throw new InvalidRefreshTokenException("Refresh token is already revoked");
        }

        userRefreshToken.setStatus(UserRefreshTokenStatus.REVOKED);

        User user = userRefreshToken.getUser();

        if (isNull(user) || user.isNotActive()) {
            throw new InvalidRefreshTokenException("Refresh token user is invalid");
        }

        return user;
    }

    public void deleteRevoked() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        int deletedCount = userRefreshTokenRepository.deleteRevoked(UserRefreshTokenStatus.REVOKED, cutoff);

        log.info("Deleted {} revoked refresh tokens", deletedCount);
    }

    // --helpers --
    private UserRefreshToken findByJti(String jti) {
        return userRefreshTokenRepository.findByJti(jti).orElse(null);
    }
}
