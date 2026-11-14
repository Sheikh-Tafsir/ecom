package com.example.ecom.user.user.service;

import com.example.ecom.auth.service.AuthTokenService;
import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.common.enums.Permission;
import com.example.ecom.common.enums.UserStatus;
import com.example.ecom.user.user.model.Role;
import com.example.ecom.user.user.model.User;
import com.example.ecom.common.service.MessageService;
import com.example.ecom.user.role.service.RoleService;
import com.example.ecom.user.user.dto.UpdateUserRequest;
import com.example.ecom.user.user.dto.UserResponse;
import com.example.ecom.user.user.dto.UserSearchResponse;
import com.example.ecom.user.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private MessageService messageService;

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private UserEventPublisher userEventPublisher;

    private UserService userService;

    private UUID userId;
    private User user;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, roleService, messageService, authTokenService, userEventPublisher);

        userId = UUID.fromString("b2c3d4e5-f6a7-8901-2345-678901abcdef");

        adminRole = new Role();
        adminRole.setId(UUID.randomUUID());
        adminRole.setName("Admin");
        adminRole.setPermissions(Set.of(Permission.ADMIN_ACCESS));

        user = new User();
        user.setId(userId);
        user.setName("Alice Johnson");
        user.setEmail("alice@example.com");
        user.setPassword("encoded_password");
        user.setImage("https://cdn.example.com/avatars/alice.jpg");
        user.setRoles(new HashSet<>(Set.of(adminRole)));
        user.setStatus(UserStatus.ACTIVE);
        user.setDeleted(false);
    }

    // ── findAll ─────────────────────────────────────────────────

    @Test
    @DisplayName("findAll should return paginated user responses")
    void findAll_usersExist_returnsPageOfUserResponses() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(user));
        when(roleService.findByName(isNull())).thenReturn(null);
        when(userRepository.findByRoleAndStatus(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(userPage);

        Page<UserResponse> result = userService.findAll(pageable, null, null, null);

        assertEquals(1, result.getTotalElements());
        assertEquals("Alice Johnson", result.getContent().get(0).getName());
        verify(userRepository).findByRoleAndStatus(isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("findAll should filter by role name and status")
    void findAll_withFilters_appliesFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(user));
        when(roleService.findByName("Admin")).thenReturn(adminRole);
        when(userRepository.findByRoleAndStatus(eq("Alice"), eq(adminRole), eq(UserStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(userPage);

        Page<UserResponse> result = userService.findAll(pageable, "Alice", "Admin", UserStatus.ACTIVE);

        assertEquals(1, result.getTotalElements());
        verify(roleService).findByName("Admin");
    }

    // ── findAllByName ───────────────────────────────────────────

    @Test
    @DisplayName("findAllByName should return list of user search responses excluding current user")
    void findAllByName_usersFound_returnsSearchResponses() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getId()).thenReturn(UUID.randomUUID());

        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        otherUser.setName("Bob Brown");
        otherUser.setImage("https://cdn.example.com/avatars/bob.jpg");

        when(userRepository.findAllByNameAndStatus(eq("Bob"), eq(UserStatus.ACTIVE), any(UUID.class), any(Pageable.class)))
                .thenReturn(List.of(otherUser));

        List<UserSearchResponse> result = userService.findAllByName("Bob", userDetails);

        assertEquals(1, result.size());
        assertEquals("Bob Brown", result.get(0).getName());
        verify(userRepository).findAllByNameAndStatus(eq("Bob"), eq(UserStatus.ACTIVE), any(UUID.class), any(Pageable.class));
    }

    // ── findAllByIds ────────────────────────────────────────────

    @Test
    @DisplayName("findAllByIds should return user search responses for given ids")
    void findAllByIds_idsProvided_returnsSearchResponses() {
        List<UUID> ids = List.of(userId);
        when(userRepository.findAllById(ids)).thenReturn(List.of(user));

        List<UserSearchResponse> result = userService.findAllByIds(ids);

        assertEquals(1, result.size());
        assertEquals("Alice Johnson", result.get(0).getName());
        verify(userRepository).findAllById(ids);
    }

    @Test
    @DisplayName("findAllByIds should return empty list when ids list is empty")
    void findAllByIds_emptyIdsList_returnsEmptyList() {
        List<UserSearchResponse> result = userService.findAllByIds(Collections.emptyList());

        assertTrue(result.isEmpty());
        verify(userRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("findAllByIds should return empty list when ids list is null")
    void findAllByIds_nullIdsList_returnsEmptyList() {
        List<UserSearchResponse> result = userService.findAllByIds(null);

        assertTrue(result.isEmpty());
        verify(userRepository, never()).findAllById(any());
    }

    // ── findById ────────────────────────────────────────────────

    @Test
    @DisplayName("findById should return user response when user exists")
    void findById_existingUser_returnsUserResponse() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse result = userService.findById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("Alice Johnson", result.getName());
        assertEquals("alice@example.com", result.getEmail());
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("findById should throw EntityNotFoundException when user not found")
    void findById_nonExistentUser_throwsEntityNotFoundException() {
        UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000020");
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        when(messageService.get(eq("error.entity.not.found"), eq("User"), eq(nonExistentId)))
                .thenReturn("User not found");

        assertThrows(EntityNotFoundException.class, () -> userService.findById(nonExistentId));

        verify(userRepository).findById(nonExistentId);
    }

    // ── update ──────────────────────────────────────────────────

    @Test
    @DisplayName("update should update user roles and revoke tokens when roles are provided")
    void update_withRoles_updatesRolesAndRevokesTokens() {
        UpdateUserRequest request = new UpdateUserRequest(Set.of("Admin"), null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleService.findByName("Admin")).thenReturn(adminRole);
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.update(userId, request);

        verify(userRepository).findById(userId);
        verify(roleService).findByName("Admin");
        verify(authTokenService).revokeAllForUser(userId);
        verify(userRepository).save(user);
        verify(userEventPublisher).publishUserChanged(userId);
    }

    @Test
    @DisplayName("update should update user status and revoke tokens when status is non-active")
    void update_withNonActiveStatus_updatesStatusAndRevokesTokens() {
        UpdateUserRequest request = new UpdateUserRequest(null, "Suspended");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.update(userId, request);

        assertEquals(UserStatus.SUSPENDED, user.getStatus());
        verify(authTokenService).revokeAllForUser(userId);
        verify(userRepository).save(user);
        verify(userEventPublisher).publishUserChanged(userId);
    }

    @Test
    @DisplayName("update should not revoke tokens when status changes to active")
    void update_withActiveStatus_doesNotRevokeTokens() {
        user.setStatus(UserStatus.SUSPENDED);
        UpdateUserRequest request = new UpdateUserRequest(null, "Active");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.update(userId, request);

        assertEquals(UserStatus.ACTIVE, user.getStatus());
        verify(authTokenService, never()).revokeAllForUser(any());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("update should not update roles when roles set is empty")
    void update_withEmptyRoles_doesNotUpdateRoles() {
        UpdateUserRequest request = new UpdateUserRequest(Collections.emptySet(), null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.update(userId, request);

        verify(roleService, never()).findByName(any());
        verify(authTokenService, never()).revokeAllForUser(any());
    }

    @Test
    @DisplayName("update should not update status when status is null")
    void update_withNullStatus_doesNotUpdateStatus() {
        UpdateUserRequest request = new UpdateUserRequest(null, null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.update(userId, request);

        assertEquals(UserStatus.ACTIVE, user.getStatus());
        verify(authTokenService, never()).revokeAllForUser(any());
    }

    @Test
    @DisplayName("update should throw EntityNotFoundException when user not found")
    void update_nonExistentUser_throwsEntityNotFoundException() {
        UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000021");
        UpdateUserRequest request = new UpdateUserRequest(Set.of("Admin"), null);
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        when(messageService.get(eq("error.entity.not.found"), eq("User"), eq(nonExistentId)))
                .thenReturn("User not found");

        assertThrows(EntityNotFoundException.class, () -> userService.update(nonExistentId, request));

        verify(userRepository, never()).save(any());
    }

    // ── delete ──────────────────────────────────────────────────

    @Test
    @DisplayName("delete should mark user as deleted and revoke tokens")
    void delete_existingUser_marksDeletedAndRevokesTokens() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.delete(userId);

        assertEquals(UserStatus.DELETED, user.getStatus());
        assertTrue(user.getDeleted());
        verify(authTokenService).revokeAllForUser(userId);
        verify(userRepository).save(user);
        verify(userEventPublisher).publishUserChanged(userId);
    }

    @Test
    @DisplayName("delete should throw EntityNotFoundException when user not found")
    void delete_nonExistentUser_throwsEntityNotFoundException() {
        UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000022");
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        when(messageService.get(eq("error.entity.not.found"), eq("User"), eq(nonExistentId)))
                .thenReturn("User not found");

        assertThrows(EntityNotFoundException.class, () -> userService.delete(nonExistentId));

        verify(userRepository, never()).save(any());
    }

    // ── banned ──────────────────────────────────────────────────

    @Test
    @DisplayName("banned should mark user as banned and revoke tokens")
    void banned_existingUser_marksBannedAndRevokesTokens() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.banned(userId);

        assertEquals(UserStatus.BANNED, user.getStatus());
        assertTrue(user.getDeleted());
        verify(authTokenService).revokeAllForUser(userId);
        verify(userRepository).save(user);
        verify(userEventPublisher).publishUserChanged(userId);
    }

    @Test
    @DisplayName("banned should throw EntityNotFoundException when user not found")
    void banned_nonExistentUser_throwsEntityNotFoundException() {
        UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000023");
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        when(messageService.get(eq("error.entity.not.found"), eq("User"), eq(nonExistentId)))
                .thenReturn("User not found");

        assertThrows(EntityNotFoundException.class, () -> userService.banned(nonExistentId));
    }

    // ── findAllAdmins ───────────────────────────────────────────

    @Test
    @DisplayName("findAllAdmins should return list of admin users")
    void findAllAdmins_adminsExist_returnsAdminList() {
        when(userRepository.findActiveUsersWithPermissions(Set.of(Permission.ADMIN_ACCESS, Permission.SUPER_ADMIN_ACCESS)))
                .thenReturn(List.of(user));

        List<User> result = userService.findAllAdmins();

        assertEquals(1, result.size());
        assertEquals("Alice Johnson", result.get(0).getName());
        verify(userRepository).findActiveUsersWithPermissions(Set.of(Permission.ADMIN_ACCESS, Permission.SUPER_ADMIN_ACCESS));
    }

    // ── delete(User, UserStatus) helper ─────────────────────────

    @Test
    @DisplayName("delete helper should set status, mark deleted, revoke tokens and save user")
    void deleteHelper_validUser_updatesStatusAndSaves() {
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.delete(user, UserStatus.DELETED);

        assertEquals(UserStatus.DELETED, user.getStatus());
        assertTrue(user.getDeleted());
        verify(authTokenService).revokeAllForUser(userId);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("delete helper should not revoke tokens when user id is null")
    void deleteHelper_nullUserId_doesNotRevokeTokens() {
        User userWithNullId = new User();
        userWithNullId.setStatus(UserStatus.ACTIVE);

        when(userRepository.save(any(User.class))).thenReturn(userWithNullId);

        userService.delete(userWithNullId, UserStatus.DELETED);

        assertEquals(UserStatus.DELETED, userWithNullId.getStatus());
        assertTrue(userWithNullId.getDeleted());
        verify(authTokenService, never()).revokeAllForUser(any());
        verify(userRepository).save(userWithNullId);
    }

    // ── publishUserChanged ──────────────────────────────────────

    @Test
    @DisplayName("publishUserChanged should handle exception from event publisher gracefully")
    void publishUserChanged_publisherThrows_handlesGracefully() {
        doThrow(new RuntimeException("Event bus down")).when(userEventPublisher).publishUserChanged(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        assertDoesNotThrow(() -> userService.delete(userId));

        verify(userEventPublisher).publishUserChanged(userId);
    }

    @Test
    @DisplayName("update should work when userEventPublisher is null")
    void update_nullEventPublisher_worksWithoutPublishing() {
        UserService serviceWithoutPublisher = new UserService(
                userRepository, roleService, messageService, authTokenService, null);

        UpdateUserRequest request = new UpdateUserRequest(null, "Suspended");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        assertDoesNotThrow(() -> serviceWithoutPublisher.update(userId, request));

        verify(userRepository).save(user);
    }

    // ── deleteNotVerifiedUsers ──────────────────────────────────

    @Test
    @DisplayName("deleteNotVerifiedUsers should call repository to delete unverified users")
    void deleteNotVerifiedUsers_invoked_callsRepositoryDelete() {
        when(userRepository.deleteNotVerifiedBefore(eq(UserStatus.NOT_VERIFIED), any(java.time.Instant.class)))
                .thenReturn(3);

        userService.deleteNotVerifiedUsers();

        verify(userRepository).deleteNotVerifiedBefore(eq(UserStatus.NOT_VERIFIED), any(java.time.Instant.class));
    }
}
