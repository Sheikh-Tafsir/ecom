package com.example.demo.product.dto;

import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Set;

public record UpdateProductRequest(

        @Size(min = 2, max = 100)
        String name,

        @DecimalMin(value = "1.0")
        BigDecimal price,

        Set<MultipartFile> images,

        Set<Long> keptImageIds
) {
}
