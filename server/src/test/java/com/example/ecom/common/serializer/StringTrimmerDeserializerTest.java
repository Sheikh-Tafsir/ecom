package com.example.ecom.common.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class StringTrimmerDeserializerTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        com.fasterxml.jackson.databind.module.SimpleModule module =
                new com.fasterxml.jackson.databind.module.SimpleModule();
        module.addDeserializer(String.class, new StringTrimmerDeserializer());
        objectMapper.registerModule(module);
    }

    @Test
    @DisplayName("Trims leading and trailing whitespace from a JSON string value")
    void deserialize_stringWithWhitespace_returnsTrimmedValue() throws IOException {
        String json = "{\"name\": \"  John Doe  \"}";
        TestDto result = objectMapper.readValue(json, TestDto.class);
        assertEquals("John Doe", result.name);
    }

    @Test
    @DisplayName("Returns string as-is when there is no whitespace")
    void deserialize_noWhitespace_returnsAsIs() throws IOException {
        String json = "{\"name\": \"Alice\"}";
        TestDto result = objectMapper.readValue(json, TestDto.class);
        assertEquals("Alice", result.name);
    }

    @Test
    @DisplayName("Returns null for a null JSON value")
    void deserialize_nullValue_returnsNull() throws IOException {
        String json = "{\"name\": null}";
        TestDto result = objectMapper.readValue(json, TestDto.class);
        assertNull(result.name);
    }

    @Test
    @DisplayName("Trims string with only whitespace to empty string")
    void deserialize_onlyWhitespace_returnsEmpty() throws IOException {
        String json = "{\"name\": \"   \"}";
        TestDto result = objectMapper.readValue(json, TestDto.class);
        assertEquals("", result.name);
    }

    static class TestDto {
        public String name;
    }
}
