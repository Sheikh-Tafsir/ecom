package com.example.demo.common.model;

import com.example.demo.common.enums.Gender;
import com.example.demo.common.enums.Permission;
import com.example.demo.common.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private Gender gender;

    private String image;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.NOT_VERIFIED;

    private Boolean deleted = false;

    public boolean isNotActive() {
        return status != UserStatus.ACTIVE;
    }

    public Set<String> getRoleValues() {
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }

    public Set<String> getPermissionValues() {
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getValue)
                .collect(Collectors.toSet());
    }
}
