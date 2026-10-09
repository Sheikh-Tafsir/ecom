package com.example.ecom.user.user.dto;

import com.example.ecom.common.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResponse {
    private UUID id;
    private String name;
    private String image;

    public UserSearchResponse(User user) {
        id = user.getId();
        name = user.getName();
        image = user.getImage();
    }
}
