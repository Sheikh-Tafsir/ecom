package com.example.demo.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(

        @NotBlank
        @Size(min = 2, max = 100)
        String name,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal price,

        @Min(0)
        int quantity,

        List<MultipartFile> images
) {
}
