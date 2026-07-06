package com.example.demo.common.utils;

import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.enums.Permission;
import com.example.demo.common.model.Role;
import com.example.demo.common.model.User;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilTest {

    @Test
    void hasPermission_singlePermission_returnsTrueIfUserHasPermission() {
        User user = createWithPermission(Permission.ADMIN_ACCESS);
        CustomUserDetails userDetails = new CustomUserDetails(user);

        assertTrue(SecurityUtil.hasPermission(Permission.ADMIN_ACCESS.getValue(), userDetails));
    }

    @Test
    void hasPermission_singlePermission_returnsFalseIfUserDoesNotHavePermission() {
        User user = createWithPermission(Permission.ADMIN_ACCESS);
        CustomUserDetails userDetails = new CustomUserDetails(user);

        assertFalse(SecurityUtil.hasPermission(Permission.SUPER_ADMIN_ACCESS.getValue(), userDetails));
    }

    @Test
    void hasPermission_listPermissions_returnsTrueIfUserHasAnyPermission() {
        User user = createWithPermission(Permission.ADMIN_ACCESS);
        CustomUserDetails userDetails = new CustomUserDetails(user);

        assertTrue(SecurityUtil.hasPermission(
                List.of(Permission.SUPER_ADMIN_ACCESS.getValue(), Permission.ADMIN_ACCESS.getValue()),
                userDetails
        ));
    }

    @Test
    void hasPermission_listPermissions_returnsFalseIfUserHasNoPermission() {
        User user = new User();
        user.setRoles(Collections.emptySet());
        CustomUserDetails userDetails = new CustomUserDetails(user);

        assertFalse(SecurityUtil.hasPermission(
                List.of(Permission.SUPER_ADMIN_ACCESS.getValue(), Permission.ADMIN_ACCESS.getValue()),
                userDetails
        ));
    }

    @Test
    void hasPermission_listPermissions_returnsTrueIfPermissionsListIsEmpty() {
        User user = createWithPermission(Permission.ADMIN_ACCESS);
        CustomUserDetails userDetails = new CustomUserDetails(user);

        assertTrue(SecurityUtil.hasPermission(Collections.emptyList(), userDetails));
    }

    @Test
    void hasPermission_listPermissions_returnsFalseIfUserDetailsIsNull() {
        assertFalse(SecurityUtil.hasPermission(List.of("READ"), null));
    }

    private User createWithPermission(Permission permission) {
        Role role = new Role();
        role.setName("ROLE_TEST");
        role.setPermissions(Set.of(permission));

        User user = new User();
        user.setRoles(Set.of(role));
        return user;
    }
}
