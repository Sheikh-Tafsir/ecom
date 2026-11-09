package com.example.ecom.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MailMessageDtoTest {

    @Test
    @DisplayName("Record constructor sets to, subject, and body")
    void constructor_validArgs_setsAllFields() {
        MailMessageDto dto = new MailMessageDto("user@example.com", "Welcome", "Hello there!");

        assertEquals("user@example.com", dto.to());
        assertEquals("Welcome", dto.subject());
        assertEquals("Hello there!", dto.body());
    }

    @Test
    @DisplayName("Record allows null values")
    void constructor_nullArgs_setsNulls() {
        MailMessageDto dto = new MailMessageDto(null, null, null);

        assertNull(dto.to());
        assertNull(dto.subject());
        assertNull(dto.body());
    }

    @Test
    @DisplayName("Two MailMessageDto with same fields are equal")
    void equals_sameFields_returnsTrue() {
        MailMessageDto dto1 = new MailMessageDto("a@b.com", "Sub", "Body");
        MailMessageDto dto2 = new MailMessageDto("a@b.com", "Sub", "Body");

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    @DisplayName("Two MailMessageDto with different fields are not equal")
    void equals_differentFields_returnsFalse() {
        MailMessageDto dto1 = new MailMessageDto("a@b.com", "Sub1", "Body");
        MailMessageDto dto2 = new MailMessageDto("a@b.com", "Sub2", "Body");

        assertNotEquals(dto1, dto2);
    }

    @Test
    @DisplayName("toString includes all fields")
    void toString_includesFields() {
        MailMessageDto dto = new MailMessageDto("test@example.com", "Test Subject", "Test Body");
        String str = dto.toString();

        assertTrue(str.contains("test@example.com"));
        assertTrue(str.contains("Test Subject"));
        assertTrue(str.contains("Test Body"));
    }
}
