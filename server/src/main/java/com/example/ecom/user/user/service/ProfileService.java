package com.example.ecom.user.user.service;

import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.common.model.User;
import com.example.ecom.common.service.fileStorage.FileStorageService;
import com.example.ecom.user.user.dto.ChangePasswordRequest;
import com.example.ecom.user.user.dto.ProfileResponse;
import com.example.ecom.user.user.dto.UpdateProfileRequest;
import com.example.ecom.user.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

import static com.example.ecom.common.utils.CacheConstants.CACHE_PROFILE;
import static com.example.ecom.common.utils.CacheConstants.CACHE_USER;
import static com.example.ecom.common.enums.UserStatus.DELETED;
import static com.example.ecom.common.utils.FileUtils.fileExists;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;

    private final UserService userService;

    private final FileStorageService fileStorageService;

    private final PasswordEncoder passwordEncoder;

    @Cacheable(value = CACHE_PROFILE, key = "#userDetails.id")
    public ProfileResponse getProfile(CustomUserDetails userDetails) {
        User user = userService.findByIdHelper(userDetails.getId());
        return new ProfileResponse(user, null);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_PROFILE, key = "#userDetails.id"),
            @CacheEvict(value = CACHE_USER, key = "#userDetails.id")
    })
    public User update(UpdateProfileRequest updateProfileRequest, CustomUserDetails userDetails) throws IOException {
        User user = userService.findByIdHelper(userDetails.getId());
        user.setName(updateProfileRequest.name());

        if (fileExists(updateProfileRequest.image())) {
            if (user.getImage() != null && !user.getImage().isEmpty()) {
                fileStorageService.deleteFileAsync(user.getImage());
            }

            String imageUrl = fileStorageService.uploadFile(updateProfileRequest.image());
            user.setImage(imageUrl);
        }

        return userRepository.save(user);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_PROFILE, key = "#userDetails.id"),
            @CacheEvict(value = CACHE_USER, key = "#userDetails.id")
    })
    public void updatePassword(ChangePasswordRequest changePasswordRequest, CustomUserDetails userDetails) {
        User user = userService.findByIdHelper(userDetails.getId());
        if (!passwordEncoder.matches(changePasswordRequest.currentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(changePasswordRequest.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_PROFILE, key = "#userDetails.id"),
            @CacheEvict(value = CACHE_USER, key = "#userDetails.id")
    })
    public void delete(CustomUserDetails userDetails) {
        userService.delete(userService.findByIdHelper(userDetails.getId()), DELETED);
    }
}
