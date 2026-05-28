package com.example.demo.user.service;

import com.example.demo.common.model.Role;
import com.example.demo.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    @PreAuthorize("hasRole('ADMIN')")
    public Role findByName(String name) {
        return roleRepository.findByName(name).orElse(null);
    }
}
