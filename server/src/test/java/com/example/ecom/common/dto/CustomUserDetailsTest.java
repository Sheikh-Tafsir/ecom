package com.example.ecom.common.dto;

import com.example.ecom.common.enums.Permission;
import com.example.ecom.common.enums.UserStatus;
import com.example.ecom.common.model.Role;
import com.example.ecom.common.model.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomUserDetailsTest {

    @Test
    @DisplayName("Construction from Claims sets id, email, status and authorities")
    void constructor_fromClaims_setsAllFields() {
        UUID userId = UUID.fromString("01927d6c-1a8f-7bfa-9ab2-c3d4e5f60718");
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(userId.toString());
        when(claims.get("email", String.class)).thenReturn("john.doe@example.com");
        when(claims.get("status", String.class)).thenReturn("Active");
        when(claims.get("permissions", List.class)).thenReturn(List.of("admin:access", "user:access"));

        CustomUserDetails details = new CustomUserDetails(claims);

        assertEquals(userId, details.getId());
        assertEquals("john.doe@example.com", details.getEmail());
        assertEquals(UserStatus.ACTIVE, details.getStatus());
        assertEquals(2, details.getAuthorities().size());
        assertEquals(userId.toString(), details.getUsername());
        assertNull(details.getPassword());
        assertTrue(details.isEnabled());
    }

    @Test
    @DisplayName("Construction from Claims with null permissions results in empty authorities")
    void constructor_fromClaimsNullPermissions_emptyAuthorities() {
        UUID userId = UUID.fromString("01927d6c-2b9f-7cfa-8ab3-d4e5f6071829");
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(userId.toString());
        when(claims.get("email", String.class)).thenReturn("jane.doe@example.com");
        when(claims.get("status", String.class)).thenReturn("Active");
        when(claims.get("permissions", List.class)).thenReturn(null);

        CustomUserDetails details = new CustomUserDetails(claims);

        assertTrue(details.getAuthorities().isEmpty());
    }

    @Test
    @DisplayName("Construction from User entity sets all fields correctly")
    void constructor_fromUser_setsAllFields() {
        UUID userId = UUID.fromString("01927d6c-3caf-7dfa-7ab4-e5f607182930");
        User user = new User();
        user.setId(userId);
        user.setEmail("admin@example.com");
        user.setStatus(UserStatus.ACTIVE);

        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        adminRole.setPermissions(Set.of(Permission.ADMIN_ACCESS));
        user.setRoles(Set.of(adminRole));

        CustomUserDetails details = new CustomUserDetails(user);

        assertEquals(userId, details.getId());
        assertEquals("admin@example.com", details.getEmail());
        assertEquals(UserStatus.ACTIVE, details.getStatus());
        assertEquals(1, details.getAuthorities().size());
        assertTrue(details.isEnabled());
    }

    @Test
    @DisplayName("isEnabled returns false for non-ACTIVE status")
    void isEnabled_suspendedUser_returnsFalse() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(UUID.randomUUID().toString());
        when(claims.get("email", String.class)).thenReturn("suspended@example.com");
        when(claims.get("status", String.class)).thenReturn("Suspended");
        when(claims.get("permissions", List.class)).thenReturn(null);

        CustomUserDetails details = new CustomUserDetails(claims);

        assertFalse(details.isEnabled());
        assertEquals(UserStatus.SUSPENDED, details.getStatus());
    }

    @Test
    @DisplayName("isEnabled returns false for NOT_VERIFIED status")
    void isEnabled_notVerifiedUser_returnsFalse() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(UUID.randomUUID().toString());
        when(claims.get("email", String.class)).thenReturn("newuser@example.com");
        when(claims.get("status", String.class)).thenReturn("Not Verified");
        when(claims.get("permissions", List.class)).thenReturn(null);

        CustomUserDetails details = new CustomUserDetails(claims);

        assertFalse(details.isEnabled());
        assertEquals(UserStatus.NOT_VERIFIED, details.getStatus());
    }

    @Test
    @DisplayName("getPassword always returns null")
    void getPassword_always_returnsNull() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(UUID.randomUUID().toString());
        when(claims.get("email", String.class)).thenReturn("user@example.com");
        when(claims.get("status", String.class)).thenReturn("Active");
        when(claims.get("permissions", List.class)).thenReturn(null);

        CustomUserDetails details = new CustomUserDetails(claims);

        assertNull(details.getPassword());
    }

    @Test
    @DisplayName("getUsername returns the UUID string")
    void getUsername_returnsUuidString() {
        UUID userId = UUID.fromString("01927d6c-4dbf-7efa-6ab5-f60718293041");
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(userId.toString());
        when(claims.get("email", String.class)).thenReturn("user@example.com");
        when(claims.get("status", String.class)).thenReturn("Active");
        when(claims.get("permissions", List.class)).thenReturn(null);

        CustomUserDetails details = new CustomUserDetails(claims);

        assertEquals(userId.toString(), details.getUsername());
    }
}
