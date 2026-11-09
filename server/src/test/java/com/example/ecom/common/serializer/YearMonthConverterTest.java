package com.example.ecom.common.serializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class YearMonthConverterTest {

    private YearMonthConverter converter;

    @BeforeEach
    void setUp() {
        converter = new YearMonthConverter();
    }

    // --- convertToDatabaseColumn ---

    @Test
    @DisplayName("Converts YearMonth to its string representation for DB column")
    void convertToDatabaseColumn_validYearMonth_returnsString() {
        YearMonth yearMonth = YearMonth.of(2024, 11);
        assertEquals("2024-11", converter.convertToDatabaseColumn(yearMonth));
    }

    @Test
    @DisplayName("Returns null when converting null YearMonth to DB column")
    void convertToDatabaseColumn_null_returnsNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    // --- convertToEntityAttribute ---

    @Test
    @DisplayName("Converts valid string to YearMonth entity attribute")
    void convertToEntityAttribute_validString_returnsYearMonth() {
        YearMonth result = converter.convertToEntityAttribute("2024-11");
        assertEquals(YearMonth.of(2024, 11), result);
    }

    @Test
    @DisplayName("Returns null when converting null DB string to entity attribute")
    void convertToEntityAttribute_null_returnsNull() {
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    @DisplayName("Throws exception for invalid YearMonth string format")
    void convertToEntityAttribute_invalidFormat_throwsException() {
        assertThrows(Exception.class, () -> converter.convertToEntityAttribute("not-a-yearmonth"));
    }
}
