package com.example.ecom.user.user.dto;

import com.example.ecom.common.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

import static com.example.ecom.common.utils.Utils.isNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    private Long id;

    private String name;

    private String email;

    private String image;

    private Set<String> roles;

    private String accessToken;

    public ProfileResponse(User user, String accessToken) {
        id = user.getId();
        name = user.getName();
        email = user.getEmail();
        image = user.getImage();
        roles = user.getRoleValues();

        if (!isNull(accessToken)) {
            this.accessToken = accessToken;
        }
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
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
}
