package com.example.demo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OtpEmailRequest(
        @NotBlank
        @Size(max = 31)
        @Email String email
) {
}
