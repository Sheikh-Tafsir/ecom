package com.example.ecom.common.service;

import com.example.ecom.user.user.repository.UserRepository;
import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.common.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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

        return new CustomUserDetails(user);
    }
}
