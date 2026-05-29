package com.example.demo.user.service;

import com.example.demo.common.model.Role;
import com.example.demo.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import static com.example.demo.common.utils.SecurityConstants.HAS_ROLE_ADMIN;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    @PreAuthorize(HAS_ROLE_ADMIN)
    public Role findByName(String name) {
        return roleRepository.findByName(name).orElse(null);
    }
}
