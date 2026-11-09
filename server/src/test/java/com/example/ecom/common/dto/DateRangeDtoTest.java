package com.example.ecom.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class DateRangeDtoTest {

    @Test
    @DisplayName("Record constructor sets fromDate and toDate correctly")
    void constructor_validDates_setsFields() {
        Instant from = Instant.parse("2025-01-01T00:00:00Z");
        Instant to = Instant.parse("2025-12-31T23:59:59Z");

        DateRangeDto dto = new DateRangeDto(from, to);

        assertEquals(from, dto.fromDate());
        assertEquals(to, dto.toDate());
    }

    @Test
    @DisplayName("Record allows null fromDate and toDate")
    void constructor_nullDates_setsNulls() {
        DateRangeDto dto = new DateRangeDto(null, null);

        assertNull(dto.fromDate());
        assertNull(dto.toDate());
    }

    @Test
    @DisplayName("Two DateRangeDto with same dates are equal")
    void equals_sameDates_returnsTrue() {
        Instant from = Instant.parse("2025-06-01T10:00:00Z");
        Instant to = Instant.parse("2025-06-30T10:00:00Z");

        DateRangeDto dto1 = new DateRangeDto(from, to);
        DateRangeDto dto2 = new DateRangeDto(from, to);

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    @DisplayName("Two DateRangeDto with different dates are not equal")
    void equals_differentDates_returnsFalse() {
        DateRangeDto dto1 = new DateRangeDto(Instant.parse("2025-01-01T00:00:00Z"), Instant.parse("2025-06-01T00:00:00Z"));
        DateRangeDto dto2 = new DateRangeDto(Instant.parse("2025-02-01T00:00:00Z"), Instant.parse("2025-06-01T00:00:00Z"));

        assertNotEquals(dto1, dto2);
    }
}
