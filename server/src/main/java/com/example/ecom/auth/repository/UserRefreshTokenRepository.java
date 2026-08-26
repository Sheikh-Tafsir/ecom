package com.example.ecom.auth.repository;

import com.example.ecom.common.enums.UserRefreshTokenStatus;
import com.example.ecom.common.model.UserRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshToken, Long> {

    Optional<UserRefreshToken> findByUser_Id(Long userId);

    Optional<UserRefreshToken> findByJti(String jti);

    @Query("SELECT urt FROM UserRefreshToken urt JOIN FETCH urt.user u JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE urt.jti = :jti")
    Optional<UserRefreshToken> findDetailsByJti(@Param("jti") String jti);

    @Modifying
    @Query("""
                UPDATE UserRefreshToken urt
                SET urt.status = :status
                WHERE urt.user.id = :userId
                  AND urt.status = 'ACTIVE'
            """)
    int revokeAllForUser(@Param("userId") Long userId, @Param("status") UserRefreshTokenStatus status);

    @Modifying
    @Query("""
                DELETE FROM UserRefreshToken urt
                WHERE urt.status = :status
                  AND urt.createdAt <= :cutoff
            """)
    int deleteRevoked(@Param("status") UserRefreshTokenStatus status, @Param("cutoff") Instant cutoff);
}
