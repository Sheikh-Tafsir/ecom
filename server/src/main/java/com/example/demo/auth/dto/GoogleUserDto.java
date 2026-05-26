package com.example.demo.auth.dto;

import lombok.Data;

@Data
public class GoogleUserDto {

    private String email;

    private boolean verified_email;

    private String name;
}
