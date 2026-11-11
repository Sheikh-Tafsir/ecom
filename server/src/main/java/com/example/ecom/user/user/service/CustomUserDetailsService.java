package com.example.ecom.user.user.service;

import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.user.user.model.User;
import com.example.ecom.user.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email).orElse(null);
        if (isNull(user)) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        List<SimpleGrantedAuthority> authorities = user.getPermissionValues().stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        return new CustomUserDetails(user.getId(), user.getEmail(), user.isEnabled(), authorities);
    }
}
