package com.example.demo.common.utils;

import com.example.demo.common.dto.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import static com.example.demo.common.utils.SecurityConstants.*;
import static com.example.demo.common.utils.Utils.isEmpty;

public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static boolean isOwnerOrAdmin(Long userId, CustomUserDetails userDetails) {
        return isOwner(userId, userDetails) || isAdmin(userDetails);
    }

    public static boolean isOwner(Long userId, CustomUserDetails userDetails) {
        return userDetails.getId().equals(userId);
    }

    public static boolean isAdmin(CustomUserDetails userDetails) {
        return hasRole(ROLE_ADMIN, userDetails) || hasRole(ROLE_SUPER_ADMIN, userDetails);
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

//    public static boolean isAdmin() {
//        return hasRole(ROLE_ADMIN) || hasRole(ROLE_SUPER_ADMIN);
//    }
//
//    public static boolean hasRole(String role) {
//        return hasRole(role, getUserDetails());
//    }

    public static CustomUserDetails getUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserDetails) {
            return (CustomUserDetails) authentication.getPrincipal();
        }

        return null;
    }

    private static String getRoleWithPrefix(String role) {
        return ROLE_PREFIX + role;
    }
}
