package com.example.ecom.product.product.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Data
public class UpdateProductRequest {

    @NotBlank
    @Size(min = 2, max = 255)
    private String name;

    @NotBlank
    @Size(min = 5, max = 1023)
    private String description;

    @NotNull
    @DecimalMin(value = "1.0")
    private BigDecimal price;

    private Set<MultipartFile> images;

    private Set<UUID> keptImageIds;

    @NotEmpty
    private Set<UUID> categoryIds;
}
