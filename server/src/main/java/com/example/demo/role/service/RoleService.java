package com.example.demo.role.service;

import com.example.demo.common.enums.Permission;
import com.example.demo.common.model.Role;
import com.example.demo.role.repository.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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

    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    public Role findById(Long id) {
        return findByIdHelper(id);
    }

    public Role findByName(String name) {
        if (!hasText(name)) {
            return null;
        }

        if (!name.startsWith(ROLE_PREFIX)) {
            name = ROLE_PREFIX + name;
        }

        return roleRepository.findByName(name).orElse(null);
    }

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

    @Transactional
    public void delete(Long id) {
        Role role = findByIdHelper(id);
        roleRepository.delete(role);
    }

    private Role findByIdHelper(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role with id: " + id + " not found"));
    }
}
