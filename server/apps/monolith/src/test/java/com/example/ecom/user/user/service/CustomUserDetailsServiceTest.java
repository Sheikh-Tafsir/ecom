package com.example.ecom.user.user.service;

import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.common.enums.Permission;
import com.example.ecom.common.enums.UserStatus;
import com.example.ecom.user.user.model.Role;
import com.example.ecom.user.user.model.User;
import com.example.ecom.user.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("loadUserByUsername returns enabled CustomUserDetails for ACTIVE user")
    void loadUserByUsername_activeUser_returnsEnabledCustomUserDetails() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("active@example.com");
        user.setStatus(UserStatus.ACTIVE);

        Role role = new Role();
        role.setName("ROLE_ADMIN");
        role.setPermissions(Set.of(Permission.ADMIN_ACCESS));
        user.setRoles(Set.of(role));

        when(userRepository.findByEmail("active@example.com")).thenReturn(Optional.of(user));

        CustomUserDetails userDetails = (CustomUserDetails) customUserDetailsService.loadUserByUsername("active@example.com");

        assertNotNull(userDetails);
        assertEquals(userId, userDetails.getId());
        assertEquals("active@example.com", userDetails.getEmail());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isActive());
        assertEquals(1, userDetails.getAuthorities().size());
    }

    @Test
    @DisplayName("loadUserByUsername returns disabled CustomUserDetails for BANNED user")
    void loadUserByUsername_bannedUser_returnsDisabledCustomUserDetails() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("banned@example.com");
        user.setStatus(UserStatus.BANNED);

        when(userRepository.findByEmail("banned@example.com")).thenReturn(Optional.of(user));

        CustomUserDetails userDetails = (CustomUserDetails) customUserDetailsService.loadUserByUsername("banned@example.com");

        assertNotNull(userDetails);
        assertFalse(userDetails.isEnabled());
        assertFalse(userDetails.isActive());
    }

    @Test
    @DisplayName("loadUserByUsername throws UsernameNotFoundException when user does not exist")
    void loadUserByUsername_notFound_throwsUsernameNotFoundException() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
                customUserDetailsService.loadUserByUsername("missing@example.com")
        );
    }
}
