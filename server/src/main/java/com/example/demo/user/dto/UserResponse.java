package com.example.demo.user.dto;

import com.example.demo.common.enums.UserStatus;
import com.example.demo.common.model.Role;
import com.example.demo.common.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;

    private String name;

    private String email;

    private String image;

    private Set<String> roles;

    private LocalDateTime createdAt;

    private UserStatus status;

    public UserResponse(User user) {
        id = user.getId();
        name = user.getName();
        email = user.getEmail();
        image = user.getImage();
        roles = user.getRoleValues();
        createdAt = user.getCreatedAt();
        status = user.getStatus();
    }
}
