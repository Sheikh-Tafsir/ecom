package com.example.demo.product.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Set;

@Data
public class CreateProductRequest {

    @NotBlank
    @Size(min = 2, max = 255)
    private String name;

    @NotBlank
    @Size(min = 5, max = 1023)
    private String description;

    @NotNull
    @DecimalMin(value = "1.0")
    private BigDecimal price;

    @NotEmpty
    private Set<MultipartFile> images;

    @NotEmpty
    private Set<Long> categoryIds;
}
