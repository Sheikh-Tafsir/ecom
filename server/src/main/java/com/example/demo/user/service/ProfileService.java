package com.example.demo.user.service;

import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.model.User;
import com.example.demo.common.service.fileStorage.FileStorageService;
import com.example.demo.user.dto.ChangePasswordRequest;
import com.example.demo.user.dto.UpdateProfileRequest;
import com.example.demo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

import static com.example.demo.common.utils.FileUtils.fileExists;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;

    private final UserService userService;

    private final FileStorageService fileStorageService;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    @CacheEvict(value = "user", key = "#userDetails.id")
    public User update(UpdateProfileRequest updateProfileRequest, CustomUserDetails userDetails) throws IOException {
        User user = userService.findByIdHelper(userDetails.getId());
        user.setName(updateProfileRequest.name());

        if (fileExists(updateProfileRequest.image())) {
            if (user.getImage() != null && !user.getImage().isEmpty()) {
                fileStorageService.deleteFile(user.getImage());
            }

            String imageUrl = fileStorageService.uploadFile(updateProfileRequest.image());
            user.setImage(imageUrl);
        }

        return userRepository.save(user);
    }

    @Transactional
    @CacheEvict(value = "user", key = "#userDetails.id")
    public void updatePassword(ChangePasswordRequest changePasswordRequest, CustomUserDetails userDetails) {
        User user = userService.findByIdHelper(userDetails.getId());
        if (!passwordEncoder.matches(changePasswordRequest.currentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(changePasswordRequest.newPassword()));
        userRepository.save(user);
    }
}
