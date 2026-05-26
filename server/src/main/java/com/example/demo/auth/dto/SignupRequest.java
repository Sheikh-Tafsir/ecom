package com.example.demo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @NotBlank
        @Size(min = 2, max = 31)
        String name,

        @NotBlank
        @Size(max = 31)
        @Email
        String email,

        @NotBlank
        @Size(min = 8, max = 15)
        String password,

        @NotBlank
        @Size(min = 8, max = 15)
        String confirmPassword
) {
}
