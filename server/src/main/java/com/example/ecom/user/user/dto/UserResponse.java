package com.example.ecom.user.user.dto;

import com.example.ecom.common.enums.UserStatus;
import com.example.ecom.common.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;

    private String name;

    private String email;

    private String image;

    private Set<String> roles;

    private Instant createdAt;

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
