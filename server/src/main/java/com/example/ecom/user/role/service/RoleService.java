package com.example.ecom.user.role.service;

import com.example.ecom.common.enums.Permission;
import com.example.ecom.common.model.Role;
import com.example.ecom.user.role.dto.RoleResponse;
import com.example.ecom.user.role.repository.RoleRepository;
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

import static com.example.ecom.common.utils.CacheConstants.CACHE_ROLE;
import static com.example.ecom.common.utils.CacheConstants.CACHE_ROLES;
import static org.springframework.util.StringUtils.hasText;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    @PreAuthorize("hasAnyAuthority(T(com.example.ecom.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Cacheable(value = CACHE_ROLES)
    public List<RoleResponse> findAll() {
        return roleRepository.findAll()
                .stream()
                .map(RoleResponse::new)
                .toList();
    }

    @PreAuthorize("hasAuthority(T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Cacheable(value = CACHE_ROLE, key = "#id")
    public RoleResponse findById(Long id) {
        return new RoleResponse(findByIdHelper(id));
    }

    @PreAuthorize("hasAuthority(T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Transactional
    @CacheEvict(value = CACHE_ROLES, allEntries = true)
    public long create(String name, Set<Permission> permissions) {
        if (findByName(name) != null) {
            throw new ValidationException("Role with same name already exists");
        }

        Role role = new Role();
        role.setName(name);
        role.setPermissions(permissions);
        roleRepository.save(role);

        return role.getId();
    }

    @PreAuthorize("hasAuthority(T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_ROLE, key = "#id"),
            @CacheEvict(value = CACHE_ROLES, allEntries = true)
    })
    public RoleResponse update(Long id, String name, Set<Permission> permissions) {
        Role role = findByIdHelper(id);

        Role roleWithSameName = findByName(name);
        if (roleWithSameName != null && !roleWithSameName.getId().equals(id)) {
            throw new ValidationException("Role with same name already exists");
        }

        role.setName(name);
        role.setPermissions(permissions);
        return new RoleResponse(roleRepository.save(role));
    }

    @PreAuthorize("hasAuthority(T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_ROLE, key = "#id"),
            @CacheEvict(value = CACHE_ROLES, allEntries = true)
    })
    public void delete(Long id) {
        Role role = findByIdHelper(id);
        roleRepository.delete(role);
    }

    public Role findByIdHelper(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + id));
    }

    public Role findByName(String name) {
        if (!hasText(name)) {
            return null;
        }

        return roleRepository.findByName(name).orElse(null);
    }
}
