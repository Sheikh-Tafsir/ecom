package com.example.ecom.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class GoogleUserDto {

    @JsonAlias({"sub", "user_id", "id"})
    private String sub;

    private String email;

    @JsonAlias({"verified_email", "email_verified"})
    private boolean emailVerified;

    private String name;

    @JsonAlias({"aud", "audience", "issued_to"})
    private String audience;
}
