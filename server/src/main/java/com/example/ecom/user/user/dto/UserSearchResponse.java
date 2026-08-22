package com.example.ecom.user.user.dto;

import com.example.ecom.common.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResponse {
    private long id;
    private String name;
    private String image;

    public UserSearchResponse(User user) {
        id = user.getId();
        name = user.getName();
        image = user.getImage();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
}
