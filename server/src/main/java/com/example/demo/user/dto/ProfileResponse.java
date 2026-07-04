package com.example.demo.user.dto;

import com.example.demo.common.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

import static com.example.demo.common.utils.Utils.isNull;

@Getter
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
}
