package com.example.demo.user.service;

import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.service.fileStorage.FileStorageService;
import com.example.demo.role.service.RoleService;
import com.example.demo.user.dto.ChangePasswordRequest;
import com.example.demo.user.dto.UpdateProfileRequest;
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
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static com.example.demo.common.enums.UserStatus.BANNED;
import static com.example.demo.common.enums.UserStatus.DELETED;
import static com.example.demo.common.utils.FileUtils.fileExists;
import static com.example.demo.common.utils.SecurityConstants.HAS_ROLE_ADMIN;
import static com.example.demo.common.utils.Utils.getValidPageable;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final RoleService roleService;

    private final PasswordEncoder passwordEncoder;

    private final FileStorageService fileStorageService;

    private final MessageService messageService;

    @PreAuthorize(HAS_ROLE_ADMIN)
    public Page<User> findAll(Pageable pageable, String name, String role, String status) {
        return userRepository.findByRoleAndStatus(name, roleService.findByName(role), UserStatus.fromValue(status), getValidPageable(pageable));
    }

    public List<UserSearchResponse> findAllByName(String name, CustomUserDetails userDetails) {
        return userRepository.findAllByNameAndStatus(name, UserStatus.ACTIVE, userDetails.getId(), getValidPageable(Pageable.unpaged()))
                .stream()
                .map(UserSearchResponse::new)
                .toList();
    }

    @PreAuthorize(HAS_ROLE_ADMIN)
    @Cacheable("usersById")
    public User findById(Long id) {
        return findByIdHelper(id);
    }

    @Transactional
    public User update(UpdateProfileRequest updateProfileRequest, CustomUserDetails userDetails) throws IOException {
        User user = findByIdHelper(userDetails.getId());
        user.setName(updateProfileRequest.name());

        if (fileExists(updateProfileRequest.image())) {
            String imageUrl = fileStorageService.uploadFile(updateProfileRequest.image());
            user.setImage(imageUrl);
        }

        return userRepository.save(user);
    }

    @PreAuthorize(HAS_ROLE_ADMIN)
    @Caching(evict = {
            @CacheEvict(value = "usersById", key = "#id")
    })
    @Transactional
    public void delete(Long id) {
        delete(findByIdHelper(id), DELETED);
    }

    @PreAuthorize(HAS_ROLE_ADMIN)
    @Caching(evict = {
            @CacheEvict(value = "usersById", key = "#id")
    })
    @Transactional
    public void banned(Long id) {
        delete(findByIdHelper(id), BANNED);
    }

    @Transactional
    public void updatePassword(ChangePasswordRequest changePasswordRequest, CustomUserDetails userDetails) {
        User user = findByIdHelper(userDetails.getId());
        if (!passwordEncoder.matches(changePasswordRequest.currentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(changePasswordRequest.newPassword()));
        userRepository.save(user);
    }

    private User findByIdHelper(Long id) {
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
