package com.example.demo.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        BigDecimal price,
        int quantity,
        List<String> images,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
