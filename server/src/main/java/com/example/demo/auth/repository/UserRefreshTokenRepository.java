package com.example.demo.auth.repository;

import com.example.demo.common.enums.UserRefreshTokenStatus;
import com.example.demo.common.model.UserRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshToken, Long> {

    Optional<UserRefreshToken> findByUser_Id(Long userId);

    Optional<UserRefreshToken> findByJti(String jti);

    @Modifying
    @Query("""
                DELETE FROM UserRefreshToken urt
                WHERE urt.status = :status
                  AND urt.createdAt <= :cutoff
            """)
    int deleteRevoked(@Param("status") UserRefreshTokenStatus status, @Param("cutoff") LocalDateTime cutoff);
}
