package com.example.demo.user.repository;

import com.example.demo.common.enums.Permission;
import com.example.demo.common.enums.UserStatus;
import com.example.demo.common.model.Role;
import com.example.demo.common.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findById(Long id);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"roles"})
    @Query("""
                SELECT DISTINCT u FROM User u
                JOIN u.roles r
                WHERE (:name IS NULL OR u.name ILIKE CONCAT('%', CAST(:name AS string), '%'))
                  AND (:role IS NULL OR r = :role)
                  AND (:status IS NULL OR u.status = :status)
            """)
    Page<User> findByRoleAndStatus(
            @Param("name") String name,
            @Param("role") Role role,
            @Param("status") UserStatus status,
            Pageable pageable
    );

    @Query("""
                SELECT u FROM User u
                WHERE (:name IS NULL OR u.name ILIKE CONCAT('%', CAST(:name AS string), '%'))
                  AND (:status IS NULL OR u.status = :status)
                  AND u.id != :userId
            """)
    List<User> findAllByNameAndStatus(
            @Param("name") String name,
            @Param("status") UserStatus status,
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Modifying
    @Query("""
                DELETE FROM User u
                WHERE u.status = :status
                  AND u.createdAt <= :cutoff
            """)
    int deleteNotVerifiedBefore(@Param("status") UserStatus status, @Param("cutoff") LocalDateTime cutoff);

    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN u.roles r
            JOIN r.permissions p
            WHERE p IN :permissions
              AND u.status = 'ACTIVE'
            """)
    List<User> findActiveUsersWithPermissions(@Param("permissions") Set<Permission> permissions);
}
