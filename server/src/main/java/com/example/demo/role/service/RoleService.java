package com.example.demo.role.service;

import com.example.demo.common.model.Role;
import com.example.demo.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.example.demo.common.utils.SecurityConstants.ROLE_PREFIX;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    public Role findByName(String name) {
        return roleRepository.findByName(ROLE_PREFIX + name).orElse(null);
    }
}
