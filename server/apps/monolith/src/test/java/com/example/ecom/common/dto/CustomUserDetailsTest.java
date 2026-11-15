package com.example.ecom.common.dto;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomUserDetailsTest {

    @Test
    @DisplayName("Construction from Claims sets id, email, active and authorities")
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
        assertTrue(details.isActive());
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
    @DisplayName("Direct constructor sets all fields correctly")
    void constructor_direct_setsAllFields() {
        UUID userId = UUID.fromString("01927d6c-3caf-7dfa-7ab4-e5f607182930");
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("admin:access");

        CustomUserDetails details = new CustomUserDetails(userId, "admin@example.com", true, List.of(authority));

        assertEquals(userId, details.getId());
        assertEquals("admin@example.com", details.getEmail());
        assertTrue(details.isActive());
        assertEquals(1, details.getAuthorities().size());
        assertTrue(details.isEnabled());
    }

    @Test
    @DisplayName("isEnabled returns false for inactive status in claims")
    void isEnabled_suspendedUser_returnsFalse() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(UUID.randomUUID().toString());
        when(claims.get("email", String.class)).thenReturn("suspended@example.com");
        when(claims.get("status", String.class)).thenReturn("Suspended");
        when(claims.get("permissions", List.class)).thenReturn(null);

        CustomUserDetails details = new CustomUserDetails(claims);

        assertFalse(details.isEnabled());
        assertFalse(details.isActive());
    }

    @Test
    @DisplayName("isEnabled returns false for NOT_VERIFIED status in claims")
    void isEnabled_notVerifiedUser_returnsFalse() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(UUID.randomUUID().toString());
        when(claims.get("email", String.class)).thenReturn("newuser@example.com");
        when(claims.get("status", String.class)).thenReturn("Not Verified");
        when(claims.get("permissions", List.class)).thenReturn(null);

        CustomUserDetails details = new CustomUserDetails(claims);

        assertFalse(details.isEnabled());
        assertFalse(details.isActive());
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

    @Test
    @DisplayName("Direct 5-arg constructor sets all fields correctly")
    void constructor_directWithAllFields_setsAllFields() {
        UUID userId = UUID.randomUUID();
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("order:read");
        CustomUserDetails details = new CustomUserDetails(userId, "John Doe", "john.doe@example.com", true, List.of(authority));

        assertEquals(userId, details.getId());
        assertEquals("John Doe", details.getName());
        assertEquals("john.doe@example.com", details.getEmail());
        assertTrue(details.isActive());
        assertEquals(1, details.getAuthorities().size());
        assertTrue(details.isEnabled());
    }

    @Test
    @DisplayName("Direct constructor with null authorities defaults to empty list")
    void constructor_directNullAuthorities_defaultsToEmptyList() {
        UUID userId = UUID.randomUUID();
        CustomUserDetails details = new CustomUserDetails(userId, "Jane Doe", "jane.doe@example.com", true, null);

        assertNotNull(details.getAuthorities());
        assertTrue(details.getAuthorities().isEmpty());
    }
}
