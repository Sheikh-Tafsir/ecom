package com.example.demo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VerifySignupOtpRequest(

        @NotBlank
        @Size(max = 31)
        @Email
        String email,

        @NotNull
        Integer otp
) {
}
