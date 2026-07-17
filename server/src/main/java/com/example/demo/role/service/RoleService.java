package com.example.demo.role.service;

import com.example.demo.common.enums.Permission;
import com.example.demo.common.model.Role;
import com.example.demo.role.dto.RoleResponse;
import com.example.demo.role.repository.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
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
    public List<RoleResponse> findAll() {
        return roleRepository.findAll()
                .stream()
                .map(RoleResponse::new)
                .toList();
    }

    @PreAuthorize("hasAuthority(T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Cacheable(value = "role", key = "#id")
    public RoleResponse findById(Long id) {
        return new RoleResponse(findByIdHelper(id));
    }

    @PreAuthorize("hasAuthority(T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Transactional
    @CacheEvict(value = "roles", allEntries = true)
    public long create(String name, Set<Permission> permissions) {
        if (!name.startsWith(ROLE_PREFIX)) {
            name = ROLE_PREFIX + name;
        }

        Role role = new Role();
        role.setName(formatName(name));
        role.setPermissions(permissions);
        roleRepository.save(role);

        return role.getId();
    }

    @PreAuthorize("hasAuthority(T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "role", key = "#id"),
            @CacheEvict(value = "roles", allEntries = true)
    })
    public RoleResponse update(Long id, String name, Set<Permission> permissions) {
        Role role = findByIdHelper(id);
        Role roleWithSameName = findByName(name);

        if (roleWithSameName != null) {
            throw new ValidationException("Role with same name already exists");
        }

        if (!name.startsWith(ROLE_PREFIX)) {
            name = ROLE_PREFIX + name;
        }
        role.setName(formatName(name));
        role.setPermissions(permissions);
        return new RoleResponse(roleRepository.save(role));
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

    private String formatName(String name) {
        return name.toUpperCase().replace(" ", "_");
    }
}
