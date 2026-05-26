package com.example.demo.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record UpdateProfileRequest(

        @NotBlank
        @Size(min = 2, max = 31)
        String name,

        MultipartFile image
) {
}
