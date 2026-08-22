package com.example.ecom.common.dto;

import com.example.ecom.common.enums.UserStatus;
import com.example.ecom.common.model.User;
import io.jsonwebtoken.Claims;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final UserStatus status;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Claims claims) {
        this.id = Long.valueOf(claims.getSubject());
        this.email = claims.get("email", String.class);
        this.status = UserStatus.fromValue(claims.get("status", String.class));

        List<String> permissions = claims.get("permissions", List.class);
        this.authorities = permissions == null
                ? List.of()
                : permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.status = user.getStatus();
        this.authorities = user.getPermissionValues().stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public UserStatus getStatus() { return status; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return id.toString();
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
