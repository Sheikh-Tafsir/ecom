package com.example.ecom.common.utils;

import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.common.enums.Permission;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilTest {

    @Test
    void hasPermission_singlePermission_returnsTrueIfUserHasPermission() {
        CustomUserDetails userDetails = createUserDetailsWithPermission(Permission.ADMIN_ACCESS);

        assertTrue(SecurityUtil.hasPermission(Permission.ADMIN_ACCESS.getValue(), userDetails));
    }

    @Test
    void hasPermission_singlePermission_returnsFalseIfUserDoesNotHavePermission() {
        CustomUserDetails userDetails = createUserDetailsWithPermission(Permission.ADMIN_ACCESS);

        assertFalse(SecurityUtil.hasPermission(Permission.SUPER_ADMIN_ACCESS.getValue(), userDetails));
    }

    @Test
    void hasPermission_listPermissions_returnsTrueIfUserHasAnyPermission() {
        CustomUserDetails userDetails = createUserDetailsWithPermission(Permission.ADMIN_ACCESS);

        assertTrue(SecurityUtil.hasPermission(
                List.of(Permission.SUPER_ADMIN_ACCESS.getValue(), Permission.ADMIN_ACCESS.getValue()),
                userDetails
        ));
    }

    @Test
    void hasPermission_listPermissions_returnsFalseIfUserHasNoPermission() {
        CustomUserDetails userDetails = createUserDetailsWithPermission(null);

        assertFalse(SecurityUtil.hasPermission(
                List.of(Permission.SUPER_ADMIN_ACCESS.getValue(), Permission.ADMIN_ACCESS.getValue()),
                userDetails
        ));
    }

    @Test
    void hasPermission_listPermissions_returnsTrueIfPermissionsListIsEmpty() {
        CustomUserDetails userDetails = createUserDetailsWithPermission(Permission.ADMIN_ACCESS);

        assertTrue(SecurityUtil.hasPermission(Collections.emptyList(), userDetails));
    }

    @Test
    void hasPermission_listPermissions_returnsFalseIfUserDetailsIsNull() {
        assertFalse(SecurityUtil.hasPermission(List.of("READ"), null));
    }

    private CustomUserDetails createUserDetailsWithPermission(Permission permission) {
        List<SimpleGrantedAuthority> authorities = permission != null
                ? Collections.singletonList(new SimpleGrantedAuthority(permission.getValue()))
                : Collections.emptyList();

        return new CustomUserDetails(
                UUID.randomUUID(),
                "test@example.com",
                true,
                authorities
        );
    }
}
