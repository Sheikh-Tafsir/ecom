package com.example.ecom.common.dto;

import java.time.Instant;

public record DateRangeDto(
        Instant fromDate,
        Instant toDate
) {
}
