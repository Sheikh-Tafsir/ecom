package com.example.demo.user.service;

import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.enums.Permission;
import com.example.demo.common.model.Role;
import com.example.demo.role.service.RoleService;
import com.example.demo.user.dto.UpdateUserRequest;
import com.example.demo.user.dto.UserResponse;
import com.example.demo.user.dto.UserSearchResponse;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.common.enums.UserStatus;
import com.example.demo.common.model.User;
import com.example.demo.common.service.MessageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.example.demo.common.enums.UserStatus.BANNED;
import static com.example.demo.common.enums.UserStatus.DELETED;
import static com.example.demo.common.utils.Utils.getValidPageable;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final RoleService roleService;

    private final MessageService messageService;

    @PreAuthorize("hasAnyAuthority(T(com.example.demo.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    public Page<UserResponse> findAll(Pageable pageable, String name, String role, UserStatus status) {
        return userRepository.findByRoleAndStatus(name, roleService.findByName(role), status, getValidPageable(pageable)).map(UserResponse::new);
    }

    public List<UserSearchResponse> findAllByName(String name, CustomUserDetails userDetails) {
        return userRepository.findAllByNameAndStatus(name, UserStatus.ACTIVE, userDetails.getId(), getValidPageable(Pageable.unpaged()))
                .stream()
                .map(UserSearchResponse::new)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority(T(com.example.demo.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Cacheable(value = "user", key = "#id")
    public UserResponse findById(Long id) {
        return new UserResponse(findByIdHelper(id));
    }

    @PreAuthorize("hasAuthority(T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @CacheEvict(value = "user", key = "#id")
    @Transactional
    public void update(long id, UpdateUserRequest request) {
        User user = findByIdHelper(id);

        if (request.roles() != null && !request.roles().isEmpty()) {
            Set<Role> roles = request.roles().stream()
                    .map(roleService::findByName)
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }

        if (request.status() != null) {
            user.setStatus(UserStatus.fromValue(request.status()));
        }

        userRepository.save(user);
    }

    @PreAuthorize("hasAuthority(T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @CacheEvict(value = "user", key = "#id")
    @Transactional
    public void delete(Long id) {
        delete(findByIdHelper(id), DELETED);
    }

    @PreAuthorize("hasAuthority(T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @CacheEvict(value = "user", key = "#id")
    @Transactional
    public void banned(Long id) {
        delete(findByIdHelper(id), BANNED);
    }

    // -- helpers --

    public List<User> findAllAdmins() {
        return userRepository.findActiveUsersWithPermissions(Set.of(Permission.ADMIN_ACCESS, Permission.SUPER_ADMIN_ACCESS));
    }

    public User findByIdHelper(Long id) {
        return userRepository.findById(id).
                orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "User", id)));
    }

    private void delete(User user, UserStatus status) {
        user.setStatus(status);
        user.setDeleted(true);
        userRepository.save(user);
    }

    @Transactional
    public void deleteNotVerifiedUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        int deletedCount = userRepository.deleteNotVerifiedBefore(UserStatus.NOT_VERIFIED, cutoff);

        log.info("Deleted {} not verified users", deletedCount);
    }
}
