package com.example.demo.common.utils;

import com.example.demo.common.dto.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static com.example.demo.common.utils.SecurityConstants.*;
import static com.example.demo.common.utils.Utils.isEmpty;

public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static boolean isAdminOrOwner(Long userId, CustomUserDetails userDetails) {
        return isAdmin(userDetails) || userDetails.getId().equals(userId);
    }

    public static boolean isAdmin(CustomUserDetails userDetails) {
        return hasRole(ROLE_ADMIN, userDetails);
    }

    public static boolean hasRole(String role, CustomUserDetails userDetails) {
        if (userDetails == null || isEmpty(userDetails.getAuthorities())) {
            return false;
        }

        return userDetails.getAuthorities().stream()
                .anyMatch(a ->
                        a.getAuthority().equals(getRoleWithPrefix(role))
                );
    }

    public static boolean isAdmin() {
        return hasRole(ROLE_ADMIN);
    }

    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || isEmpty(auth.getAuthorities())) {
            return false;
        }

        return auth.getAuthorities().stream()
                .anyMatch(a ->
                        a.getAuthority().equals(getRoleWithPrefix(role))
                );

    }

    private static String getRoleWithPrefix(String role) {
        return ROLE_PREFIX + role;
    }
}
