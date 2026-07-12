package com.example.demo.role.service;

import com.example.demo.common.enums.Permission;
import com.example.demo.common.model.Role;
import com.example.demo.role.repository.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
    @Cacheable(value = "roles")
    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    @PreAuthorize("hasAuthority(T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Cacheable(value = "role", key = "#id")
    public Role findById(Long id) {
        return roleRepository.findDetailsById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role with id: " + id + " not found"));
    }

    @PreAuthorize("hasAuthority(T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Transactional
    @CacheEvict(value = "roles", allEntries = true)
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
    @Caching(evict = {
            @CacheEvict(value = "role", key = "#id"),
            @CacheEvict(value = "roles", allEntries = true)
    })
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
    @Caching(evict = {
            @CacheEvict(value = "role", key = "#id"),
            @CacheEvict(value = "roles", allEntries = true)
    })
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
