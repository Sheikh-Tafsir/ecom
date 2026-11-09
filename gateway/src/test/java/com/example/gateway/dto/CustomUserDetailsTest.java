package com.example.gateway.dto;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsTest {

    private static final String USER_ID = "a3f1b2c4-5d6e-7f8a-9b0c-1d2e3f4a5b6c";
    private static final String USER_EMAIL = "john.doe@example.com";

    @Mock
    private Claims claims;

    @BeforeEach
    void setUp() {
        // default setup for an active user with permissions
    }

    private void setupClaims(String id, String email, String status, List<String> permissions) {
        when(claims.getSubject()).thenReturn(id);
        when(claims.get("email", String.class)).thenReturn(email);
        when(claims.get("status", String.class)).thenReturn(status);
        when(claims.get("permissions", List.class)).thenReturn(permissions);
    }

    @Test
    @DisplayName("Constructor extracts id from claims subject")
    void constructor_validClaims_extractsId() {
        setupClaims(USER_ID, USER_EMAIL, "Active", List.of("ROLE_USER"));

        CustomUserDetails details = new CustomUserDetails(claims);
        assertEquals(USER_ID, details.getId());
    }

    @Test
    @DisplayName("Constructor extracts email from claims")
    void constructor_validClaims_extractsEmail() {
        setupClaims(USER_ID, USER_EMAIL, "Active", List.of("ROLE_USER"));

        CustomUserDetails details = new CustomUserDetails(claims);
        assertEquals(USER_EMAIL, details.getEmail());
    }

    @Test
    @DisplayName("Constructor extracts ACTIVE status from claims")
    void constructor_activeStatus_setsActiveStatus() {
        setupClaims(USER_ID, USER_EMAIL, "Active", List.of("ROLE_USER"));

        CustomUserDetails details = new CustomUserDetails(claims);
        assertEquals(UserStatus.ACTIVE, details.getStatus());
    }

    @Test
    @DisplayName("Constructor maps permissions to GrantedAuthority list")
    void constructor_withPermissions_mapsToAuthorities() {
        setupClaims(USER_ID, USER_EMAIL, "Active", List.of("ROLE_USER", "products:read", "orders:write"));

        CustomUserDetails details = new CustomUserDetails(claims);

        List<String> authorityStrings = details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertEquals(3, authorityStrings.size());
        assertTrue(authorityStrings.contains("ROLE_USER"));
        assertTrue(authorityStrings.contains("products:read"));
        assertTrue(authorityStrings.contains("orders:write"));
    }

    @Test
    @DisplayName("Constructor with null permissions returns empty authorities")
    void constructor_nullPermissions_returnsEmptyAuthorities() {
        setupClaims(USER_ID, USER_EMAIL, "Active", null);

        CustomUserDetails details = new CustomUserDetails(claims);
        assertNotNull(details.getAuthorities());
        assertTrue(details.getAuthorities().isEmpty());
    }

    @Test
    @DisplayName("getPassword always returns null")
    void getPassword_always_returnsNull() {
        setupClaims(USER_ID, USER_EMAIL, "Active", List.of("ROLE_USER"));

        CustomUserDetails details = new CustomUserDetails(claims);
        assertNull(details.getPassword());
    }

    @Test
    @DisplayName("getUsername returns the user id as string")
    void getUsername_always_returnsIdAsString() {
        setupClaims(USER_ID, USER_EMAIL, "Active", List.of("ROLE_USER"));

        CustomUserDetails details = new CustomUserDetails(claims);
        assertEquals(USER_ID, details.getUsername());
    }

    @Test
    @DisplayName("isEnabled returns true when status is ACTIVE")
    void isEnabled_activeStatus_returnsTrue() {
        setupClaims(USER_ID, USER_EMAIL, "Active", List.of("ROLE_USER"));

        CustomUserDetails details = new CustomUserDetails(claims);
        assertTrue(details.isEnabled());
    }

    @Test
    @DisplayName("isEnabled returns false when status is SUSPENDED")
    void isEnabled_suspendedStatus_returnsFalse() {
        setupClaims(USER_ID, USER_EMAIL, "Suspended", List.of());

        CustomUserDetails details = new CustomUserDetails(claims);
        assertFalse(details.isEnabled());
    }

    @Test
    @DisplayName("isEnabled returns false when status is BANNED")
    void isEnabled_bannedStatus_returnsFalse() {
        setupClaims(USER_ID, USER_EMAIL, "Banned", List.of());

        CustomUserDetails details = new CustomUserDetails(claims);
        assertFalse(details.isEnabled());
    }

    @Test
    @DisplayName("isEnabled returns false when status is INACTIVE")
    void isEnabled_inactiveStatus_returnsFalse() {
        setupClaims(USER_ID, USER_EMAIL, "Inactive", List.of());

        CustomUserDetails details = new CustomUserDetails(claims);
        assertFalse(details.isEnabled());
    }

    @Test
    @DisplayName("isEnabled returns false when status is NOT_VERIFIED")
    void isEnabled_notVerifiedStatus_returnsFalse() {
        setupClaims(USER_ID, USER_EMAIL, "Not Verified", List.of());

        CustomUserDetails details = new CustomUserDetails(claims);
        assertFalse(details.isEnabled());
    }

    @Test
    @DisplayName("isEnabled returns false when status is DELETED")
    void isEnabled_deletedStatus_returnsFalse() {
        setupClaims(USER_ID, USER_EMAIL, "Deleted", List.of());

        CustomUserDetails details = new CustomUserDetails(claims);
        assertFalse(details.isEnabled());
    }

    @Test
    @DisplayName("Constructor with empty permissions list returns empty authorities")
    void constructor_emptyPermissions_returnsEmptyAuthorities() {
        setupClaims(USER_ID, USER_EMAIL, "Active", List.of());

        CustomUserDetails details = new CustomUserDetails(claims);
        assertNotNull(details.getAuthorities());
        assertTrue(details.getAuthorities().isEmpty());
    }
}
