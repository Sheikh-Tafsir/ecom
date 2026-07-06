package com.example.demo.role.service;

import com.example.demo.common.enums.Permission;
import com.example.demo.common.model.Role;
import com.example.demo.role.repository.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static com.example.demo.common.utils.SecurityConstants.ROLE_PREFIX;
import static org.springframework.util.StringUtils.hasText;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    @PreAuthorize("hasAnyAuthority(T(com.example.demo.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    @PreAuthorize("hasAuthority(T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    public Role findById(Long id) {
        return findByIdHelper(id);
    }

    @PreAuthorize("hasAuthority(T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Transactional
    public Role create(String name, Set<Permission> permissions) {
        if (!name.startsWith(ROLE_PREFIX)) {
            name = ROLE_PREFIX + name;
        }

        Role role = new Role();
        role.setName(name);
        role.setPermissions(permissions);
        return roleRepository.save(role);
    }

    @PreAuthorize("hasAuthority(T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Transactional
    public Role update(Long id, String name, Set<Permission> permissions) {
        Role role = findByIdHelper(id);
        if (!name.startsWith(ROLE_PREFIX)) {
            name = ROLE_PREFIX + name;
        }
        role.setName(name);
        role.setPermissions(permissions);
        return roleRepository.save(role);
    }

    @PreAuthorize("hasAuthority(T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Transactional
    public void delete(Long id) {
        Role role = findByIdHelper(id);
        roleRepository.delete(role);
    }

    // helpers
    public Role findByName(String name) {
        if (!hasText(name)) {
            return null;
        }

        if (!name.startsWith(ROLE_PREFIX)) {
            name = ROLE_PREFIX + name;
        }

        return roleRepository.findByName(name).orElse(null);
    }

    private Role findByIdHelper(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role with id: " + id + " not found"));
    }
}
