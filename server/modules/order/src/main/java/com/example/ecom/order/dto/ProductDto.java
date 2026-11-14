package com.example.ecom.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductDto(UUID id, String name, String image, BigDecimal price, int quantity) {
}
