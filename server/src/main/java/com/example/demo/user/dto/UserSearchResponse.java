package com.example.demo.user.dto;

import com.example.demo.common.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
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
