package com.example.demo.user.service;

import com.example.demo.user.dto.ChangePasswordRequest;
import com.example.demo.user.dto.UpdateProfileRequest;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.common.enums.UserStatus;
import com.example.demo.common.model.User;
import com.example.demo.common.service.CloudinaryService;
import com.example.demo.common.service.MessageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static com.example.demo.common.utils.FileUtils.fileExists;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final RoleService roleService;

    private final PasswordEncoder passwordEncoder;

    private final CloudinaryService cloudinaryService;

    private final MessageService messageService;

    public Page<User> findAll(Pageable pageable, String role, String status) {
        return userRepository.findByRoleAndStatus(roleService.findByName(role), UserStatus.valueOf(status.toUpperCase()), pageable);
    }

    @Cacheable("usersById")
    public User findById(Long id) {
        return findByIdHelper(id);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Caching(evict = {
            @CacheEvict(value = "usersById", key = "#id")
    })
    @Transactional
    public void delete(Long id) {
        User user = findByIdHelper(id);
        userRepository.delete(user);
    }

    @Transactional
    public void updateProfile(UpdateProfileRequest updateProfileRequest, String email) throws IOException {
        User user = findByEmail(email);
        user.setName(updateProfileRequest.name());

        if (fileExists(updateProfileRequest.image())) {
            String imageUrl = cloudinaryService.uploadFile(updateProfileRequest.image());
            user.setImage(imageUrl);
        }

        userRepository.save(user);
    }

    private User findByIdHelper(Long id) {
        return userRepository.findById(id).
                orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "User", id)));
    }

    @Transactional
    public void deleteNotVerifiedUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        List<User> notVerifiedUserList = userRepository.findAllNotVerifiedBefore(cutoff);

        if (notVerifiedUserList.isEmpty()) {
            return;
        }

        log.info("Not verified {} users available for deletion", notVerifiedUserList.size());
        userRepository.deleteAll(notVerifiedUserList);

        log.info("deletion complete");
    }

    @Transactional
    public void updatePassword(ChangePasswordRequest changePasswordRequest, String email) {
        User user = findByEmail(email);
        if (!passwordEncoder.matches(changePasswordRequest.currentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current newPassword is incorrect");
        }

        user.setPassword(passwordEncoder.encode(changePasswordRequest.newPassword()));
        userRepository.save(user);
    }
}
