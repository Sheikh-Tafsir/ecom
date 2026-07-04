package com.example.demo.auth.service;

import com.example.demo.auth.repository.UserRefreshTokenRepository;
import com.example.demo.common.model.User;
import com.example.demo.common.model.UserRefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserRefreshTokenService {

    private final UserRefreshTokenRepository userRefreshTokenRepository;

    public UserRefreshToken findByUser(User user) {
        return userRefreshTokenRepository.findByUser_Id(user.getId()).orElse(null);
    }

    @Transactional
    public void create(User user, String refreshToken) {
        UserRefreshToken userRefreshToken = findByUser(user);
        if (userRefreshToken == null) {
            userRefreshToken = new UserRefreshToken();
            userRefreshToken.setUser(user);
        }

        userRefreshToken.setToken(refreshToken);
        userRefreshTokenRepository.save(userRefreshToken);
    }

    @Transactional
    public void delete(User user) {
        userRefreshTokenRepository.deleteByUser_Id(user.getId());
    }
}
