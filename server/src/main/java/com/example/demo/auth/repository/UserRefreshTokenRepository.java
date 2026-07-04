package com.example.demo.auth.repository;

import com.example.demo.common.model.UserRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshToken, Long> {

    Optional<UserRefreshToken> findByUser_Id(Long userId);

    void deleteByUser_Id(Long userId);
}
