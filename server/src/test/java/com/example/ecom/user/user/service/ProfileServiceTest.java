package com.example.ecom.user.user.service;

import com.example.ecom.auth.service.AuthTokenService;
import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.common.enums.UserStatus;
import com.example.ecom.user.user.model.Role;
import com.example.ecom.user.user.model.User;
import com.example.ecom.common.service.fileStorage.FileStorageService;
import com.example.ecom.user.user.dto.ChangePasswordRequest;
import com.example.ecom.user.user.dto.ProfileResponse;
import com.example.ecom.user.user.dto.UpdateProfileRequest;
import com.example.ecom.user.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthTokenService authTokenService;

    @InjectMocks
    private ProfileService profileService;

    private UUID userId;
    private User user;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        userId = UUID.fromString("c3d4e5f6-a7b8-9012-3456-789012abcdef");

        Role userRole = new Role();
        userRole.setName("User");
        userRole.setPermissions(Set.of(com.example.ecom.common.enums.Permission.USER));

        user = new User();
        user.setId(userId);
        user.setName("Charlie Davis");
        user.setEmail("charlie@example.com");
        user.setPassword("$2a$10$encodedPassword123");
        user.setImage("https://cdn.example.com/avatars/charlie.jpg");
        user.setRoles(new HashSet<>(Set.of(userRole)));
        user.setStatus(UserStatus.ACTIVE);

        userDetails = mock(CustomUserDetails.class);
        when(userDetails.getId()).thenReturn(userId);
    }

    // ── getProfile ──────────────────────────────────────────────

    @Test
    @DisplayName("getProfile should return profile response for authenticated user")
    void getProfile_authenticatedUser_returnsProfileResponse() {
        when(userService.findByIdHelper(userId)).thenReturn(user);

        ProfileResponse result = profileService.getProfile(userDetails);

        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("Charlie Davis", result.getName());
        assertEquals("charlie@example.com", result.getEmail());
        assertNull(result.getAccessToken());
        verify(userService).findByIdHelper(userId);
    }

    // ── update ──────────────────────────────────────────────────

    @Test
    @DisplayName("update should update name without image when no image is provided")
    void update_noImage_updatesNameOnly() throws IOException {
        UpdateProfileRequest request = new UpdateProfileRequest("Charlie Updated", null);

        when(userService.findByIdHelper(userId)).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = profileService.update(request, userDetails);

        assertNotNull(result);
        assertEquals("Charlie Updated", result.getName());
        verify(userService).findByIdHelper(userId);
        verify(userRepository).save(user);
        verify(fileStorageService, never()).uploadFile(any());
    }

    @Test
    @DisplayName("update should upload new image and delete old one when image is provided")
    void update_withImage_uploadsNewAndDeletesOld() throws IOException {
        MultipartFile mockImage = mock(MultipartFile.class);
        when(mockImage.isEmpty()).thenReturn(false);

        UpdateProfileRequest request = new UpdateProfileRequest("Charlie Updated", mockImage);

        when(userService.findByIdHelper(userId)).thenReturn(user);
        when(fileStorageService.uploadFile(mockImage)).thenReturn("https://cdn.example.com/avatars/charlie-new.jpg");
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = profileService.update(request, userDetails);

        assertNotNull(result);
        verify(fileStorageService).deleteFileAsync("https://cdn.example.com/avatars/charlie.jpg");
        verify(fileStorageService).uploadFile(mockImage);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("update should upload image without deleting old one when old image is null")
    void update_withImageAndNoOldImage_uploadsNewOnly() throws IOException {
        user.setImage(null);
        MultipartFile mockImage = mock(MultipartFile.class);
        when(mockImage.isEmpty()).thenReturn(false);

        UpdateProfileRequest request = new UpdateProfileRequest("Charlie Updated", mockImage);

        when(userService.findByIdHelper(userId)).thenReturn(user);
        when(fileStorageService.uploadFile(mockImage)).thenReturn("https://cdn.example.com/avatars/new.jpg");
        when(userRepository.save(any(User.class))).thenReturn(user);

        profileService.update(request, userDetails);

        verify(fileStorageService, never()).deleteFileAsync(any());
        verify(fileStorageService).uploadFile(mockImage);
    }

    @Test
    @DisplayName("update should not delete old image when old image is empty string")
    void update_withImageAndEmptyOldImage_uploadsNewOnly() throws IOException {
        user.setImage("");
        MultipartFile mockImage = mock(MultipartFile.class);
        when(mockImage.isEmpty()).thenReturn(false);

        UpdateProfileRequest request = new UpdateProfileRequest("Charlie Updated", mockImage);

        when(userService.findByIdHelper(userId)).thenReturn(user);
        when(fileStorageService.uploadFile(mockImage)).thenReturn("https://cdn.example.com/avatars/new.jpg");
        when(userRepository.save(any(User.class))).thenReturn(user);

        profileService.update(request, userDetails);

        verify(fileStorageService, never()).deleteFileAsync(any());
        verify(fileStorageService).uploadFile(mockImage);
    }

    @Test
    @DisplayName("update should handle empty MultipartFile without uploading")
    void update_withEmptyMultipartFile_skipsUpload() throws IOException {
        MultipartFile emptyImage = mock(MultipartFile.class);
        when(emptyImage.isEmpty()).thenReturn(true);

        UpdateProfileRequest request = new UpdateProfileRequest("Charlie Updated", emptyImage);

        when(userService.findByIdHelper(userId)).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);

        profileService.update(request, userDetails);

        verify(fileStorageService, never()).uploadFile(any());
        verify(fileStorageService, never()).deleteFileAsync(any());
    }

    // ── updatePassword ──────────────────────────────────────────

    @Test
    @DisplayName("updatePassword should update password and revoke tokens when current password matches")
    void updatePassword_correctCurrentPassword_updatesAndRevokesTokens() {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass123", "NewPass456", "NewPass456");

        when(userService.findByIdHelper(userId)).thenReturn(user);
        when(passwordEncoder.matches("OldPass123", user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("NewPass456")).thenReturn("$2a$10$newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        profileService.updatePassword(request, userDetails);

        assertEquals("$2a$10$newEncodedPassword", user.getPassword());
        verify(passwordEncoder).matches("OldPass123", "$2a$10$encodedPassword123");
        verify(passwordEncoder).encode("NewPass456");
        verify(userRepository).save(user);
        verify(authTokenService).revokeAllForUser(userId);
    }

    @Test
    @DisplayName("updatePassword should throw BadCredentialsException when current password is incorrect")
    void updatePassword_incorrectCurrentPassword_throwsBadCredentialsException() {
        ChangePasswordRequest request = new ChangePasswordRequest("WrongPass", "NewPass456", "NewPass456");

        when(userService.findByIdHelper(userId)).thenReturn(user);
        when(passwordEncoder.matches("WrongPass", user.getPassword())).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> profileService.updatePassword(request, userDetails));

        verify(passwordEncoder).matches("WrongPass", "$2a$10$encodedPassword123");
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
        verify(authTokenService, never()).revokeAllForUser(any());
    }

    // ── delete ──────────────────────────────────────────────────

    @Test
    @DisplayName("delete should delegate to userService to mark user as deleted")
    void delete_authenticatedUser_delegatesToUserServiceDelete() {
        when(userService.findByIdHelper(userId)).thenReturn(user);

        profileService.delete(userDetails);

        verify(userService).findByIdHelper(userId);
        verify(userService).delete(user, UserStatus.DELETED);
    }
}
