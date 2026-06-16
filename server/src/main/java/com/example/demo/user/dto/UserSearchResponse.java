package com.example.demo.user.dto;

import com.example.demo.common.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserSearchResponse {
    long id;
    String name;
    String image;

    public UserSearchResponse(User user) {
        id = user.getId();
        name = user.getName();
        image = user.getImage();
    }
}
