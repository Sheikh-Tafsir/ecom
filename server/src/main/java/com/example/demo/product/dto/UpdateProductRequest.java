package com.example.demo.product.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Set;

@Data
public class UpdateProductRequest {

    @NotBlank
    @Size(min = 2, max = 255)
    String name;

    @NotBlank
    @Size(min = 5, max = 1023)
    private String description;

    @NotNull
    @DecimalMin(value = "1.0")
    BigDecimal price;

    Set<MultipartFile> images;

    Set<Long> keptImageIds;

    @NotEmpty
    Set<Long> categoryIds;
}
