package com.example.ecom.common.config;

import com.example.ecom.common.dto.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditorAwareImplTest {

    @InjectMocks
    private AuditorAwareImpl auditorAware;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Returns user ID when authentication contains CustomUserDetails")
    void getCurrentAuditor_authenticatedWithCustomUserDetails_returnsUserId() {
        UUID userId = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getId()).thenReturn(userId);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<UUID> auditor = auditorAware.getCurrentAuditor();

        assertTrue(auditor.isPresent());
        assertEquals(userId, auditor.get());
    }

    @Test
    @DisplayName("Returns empty when there is no authentication")
    void getCurrentAuditor_noAuthentication_returnsEmpty() {
        Optional<UUID> auditor = auditorAware.getCurrentAuditor();

        assertTrue(auditor.isEmpty());
    }

    @Test
    @DisplayName("Returns empty when authentication is AnonymousAuthenticationToken")
    void getCurrentAuditor_anonymousAuth_returnsEmpty() {
        AnonymousAuthenticationToken anonymousAuth = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(anonymousAuth);

        Optional<UUID> auditor = auditorAware.getCurrentAuditor();

        assertTrue(auditor.isEmpty());
    }

    @Test
    @DisplayName("Returns empty when principal is not CustomUserDetails")
    void getCurrentAuditor_nonCustomPrincipal_returnsEmpty() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("plainStringPrincipal", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<UUID> auditor = auditorAware.getCurrentAuditor();

        assertTrue(auditor.isEmpty());
    }
}
