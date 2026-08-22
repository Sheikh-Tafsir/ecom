package com.example.ecom.user.user.dto;

import com.example.ecom.common.enums.UserStatus;
import com.example.ecom.common.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
}
