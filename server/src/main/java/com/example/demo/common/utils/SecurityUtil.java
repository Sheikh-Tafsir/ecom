package com.example.demo.common.utils;

import com.example.demo.common.dto.CustomUserDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import static com.example.demo.common.utils.Utils.isEmpty;

public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static boolean isOwner(Long userId, CustomUserDetails userDetails) {
        return userDetails.getId().equals(userId);
    }

    public static boolean hasPermission(String permission, CustomUserDetails userDetails) {
        if (userDetails == null || isEmpty(userDetails.getAuthorities())) {
            return false;
        }

        return userDetails.getAuthorities().stream()
                .anyMatch(a ->
                        a.getAuthority().equals(permission)
                );
    }

    public static CustomUserDetails getUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserDetails) {
            return (CustomUserDetails) authentication.getPrincipal();
        }

        return null;
    }

    public static void throwAccessException(Long ownerId, Long userId, String resource, Object resourceValue) {
        throw new AccessDeniedException("User: " + userId + "attempted to access "+ resource + ": " + resourceValue.toString() + " owned by:" + ownerId);
    }
}
