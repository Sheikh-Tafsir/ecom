package com.example.ecom.user.user.dto;

import com.example.ecom.common.enums.UserStatus;
import com.example.ecom.common.model.Role;
import com.example.ecom.common.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserDtoTest {

    private UUID userId;
    private User user;
    private Instant now;

    @BeforeEach
    void setUp() {
        userId = UUID.fromString("01927d73-097f-7afa-9ae3-526374859607");
        now = Instant.parse("2025-03-20T09:00:00Z");

        user = new User();
        user.setId(userId);
        user.setName("Emily Johnson");
        user.setEmail("emily.j@example.com");
        user.setPassword("hashed_password");
        user.setImage("https://cdn.example.com/emily.jpg");
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(now);

        Role userRole = new Role();
        userRole.setName("USER");
        user.setRoles(Set.of(userRole));
    }

    @Nested
    @DisplayName("UserResponse Tests")
    class UserResponseTest {

        @Test
        @DisplayName("Construction from User entity maps all fields")
        void constructor_fromEntity_setsAllFields() {
            UserResponse response = new UserResponse(user);

            assertEquals(userId, response.getId());
            assertEquals("Emily Johnson", response.getName());
            assertEquals("emily.j@example.com", response.getEmail());
            assertEquals("https://cdn.example.com/emily.jpg", response.getImage());
            assertEquals(Set.of("USER"), response.getRoles());
            assertEquals(now, response.getCreatedAt());
            assertEquals(UserStatus.ACTIVE, response.getStatus());
        }

        @Test
        @DisplayName("No-args constructor creates empty response")
        void noArgsConstructor_createsEmpty() {
            UserResponse response = new UserResponse();
            assertNull(response.getId());
            assertNull(response.getName());
            assertNull(response.getEmail());
        }
    }

    @Nested
    @DisplayName("UserSearchResponse Tests")
    class UserSearchResponseTest {

        @Test
        @DisplayName("Construction from User entity maps id, name, image")
        void constructor_fromEntity_setsFields() {
            UserSearchResponse response = new UserSearchResponse(user);

            assertEquals(userId, response.getId());
            assertEquals("Emily Johnson", response.getName());
            assertEquals("https://cdn.example.com/emily.jpg", response.getImage());
        }

        @Test
        @DisplayName("No-args constructor creates empty response")
        void noArgsConstructor_createsEmpty() {
            UserSearchResponse response = new UserSearchResponse();
            assertNull(response.getId());
            assertNull(response.getName());
        }
    }

    @Nested
    @DisplayName("ProfileResponse Tests")
    class ProfileResponseTest {

        @Test
        @DisplayName("Construction from User with accessToken maps all fields")
        void constructor_fromUserWithToken_setsAllFields() {
            ProfileResponse response = new ProfileResponse(user, "jwt-access-token-12345");

            assertEquals(userId, response.getId());
            assertEquals("Emily Johnson", response.getName());
            assertEquals("emily.j@example.com", response.getEmail());
            assertEquals("https://cdn.example.com/emily.jpg", response.getImage());
            assertEquals(Set.of("USER"), response.getRoles());
            assertEquals("jwt-access-token-12345", response.getAccessToken());
        }

        @Test
        @DisplayName("Construction from User with null accessToken does not set token")
        void constructor_fromUserNullToken_noToken() {
            ProfileResponse response = new ProfileResponse(user, null);

            assertNull(response.getAccessToken());
        }

        @Test
        @DisplayName("Construction from User with empty accessToken does not set token")
        void constructor_fromUserEmptyToken_noToken() {
            ProfileResponse response = new ProfileResponse(user, "");

            assertNull(response.getAccessToken());
        }

        @Test
        @DisplayName("Setters update all fields")
        void setters_allFields_updateCorrectly() {
            ProfileResponse response = new ProfileResponse();
            UUID newId = UUID.fromString("01927d73-1a8f-7bfa-8ae4-637485960718");

            response.setId(newId);
            response.setName("Updated Name");
            response.setEmail("updated@example.com");
            response.setImage("new-img.jpg");
            response.setRoles(Set.of("ADMIN"));
            response.setAccessToken("new-token");

            assertEquals(newId, response.getId());
            assertEquals("Updated Name", response.getName());
            assertEquals("updated@example.com", response.getEmail());
            assertEquals("new-img.jpg", response.getImage());
            assertEquals(Set.of("ADMIN"), response.getRoles());
            assertEquals("new-token", response.getAccessToken());
        }
    }

    @Nested
    @DisplayName("ChangePasswordRequest Tests")
    class ChangePasswordRequestTest {

        @Test
        @DisplayName("Record constructor sets all password fields")
        void constructor_validArgs_setsFields() {
            ChangePasswordRequest request = new ChangePasswordRequest(
                    "oldPass123", "newPass456", "newPass456"
            );

            assertEquals("oldPass123", request.currentPassword());
            assertEquals("newPass456", request.newPassword());
            assertEquals("newPass456", request.confirmNewPassword());
        }
    }

    @Nested
    @DisplayName("UpdateProfileRequest Tests")
    class UpdateProfileRequestTest {

        @Test
        @DisplayName("Record constructor sets name and image")
        void constructor_validArgs_setsFields() {
            UpdateProfileRequest request = new UpdateProfileRequest("New Name", null);

            assertEquals("New Name", request.name());
            assertNull(request.image());
        }
    }

    @Nested
    @DisplayName("UpdateUserRequest Tests")
    class UpdateUserRequestTest {

        @Test
        @DisplayName("Record constructor sets roles and status")
        void constructor_validArgs_setsFields() {
            UpdateUserRequest request = new UpdateUserRequest(Set.of("ADMIN", "USER"), "ACTIVE");

            assertEquals(Set.of("ADMIN", "USER"), request.roles());
            assertEquals("ACTIVE", request.status());
        }

        @Test
        @DisplayName("Record allows null values")
        void constructor_nullArgs_allowsNulls() {
            UpdateUserRequest request = new UpdateUserRequest(null, null);
            assertNull(request.roles());
            assertNull(request.status());
        }
    }
}
