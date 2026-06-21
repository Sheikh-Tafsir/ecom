package com.example.demo.common.dto;

import java.time.LocalDateTime;

public record DateRangeDto(
        LocalDateTime fromDate,
        LocalDateTime toDate
) {
}
