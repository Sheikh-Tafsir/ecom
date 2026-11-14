package com.example.ecom.common.serializer;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageImplJacksonSerializerTest {

    @BeforeEach
    void setUp() {
        // No shared state
    }

    @Test
    @DisplayName("No-arg constructor creates empty page")
    void noArgConstructor_createsEmptyPage() {
        PageImplJacksonSerializer<String> page = new PageImplJacksonSerializer<>();

        assertTrue(page.getContent().isEmpty());
        assertEquals(0, page.getTotalElements());
    }

    @Test
    @DisplayName("Content-only constructor creates page with given content")
    void contentConstructor_createsPageWithContent() {
        List<String> content = List.of("item1", "item2", "item3");
        PageImplJacksonSerializer<String> page = new PageImplJacksonSerializer<>(content);

        assertEquals(3, page.getContent().size());
        assertEquals(3, page.getTotalElements());
        assertEquals("item1", page.getContent().get(0));
    }

    @Test
    @DisplayName("Pageable constructor creates page with pageable and total elements")
    void pageableConstructor_createsPagedResult() {
        List<String> content = List.of("item1", "item2");
        Pageable pageable = PageRequest.of(0, 10);

        PageImplJacksonSerializer<String> page = new PageImplJacksonSerializer<>(content, pageable, 50);

        assertEquals(2, page.getContent().size());
        assertEquals(50, page.getTotalElements());
        assertEquals(0, page.getNumber());
        assertEquals(10, page.getSize());
    }

    @Test
    @DisplayName("JSON creator constructor builds page from individual properties")
    void jsonCreatorConstructor_createsPageFromProperties() {
        List<String> content = List.of("product-a", "product-b");

        PageImplJacksonSerializer<String> page = new PageImplJacksonSerializer<>(
                content, 0, 10, 100L, null, false, 10, null, true, 2
        );

        assertEquals(2, page.getContent().size());
        assertEquals(100, page.getTotalElements());
        assertEquals(0, page.getNumber());
        assertEquals(10, page.getSize());
    }

    @Test
    @DisplayName("Empty content list with pageable creates valid empty page")
    void pageableConstructor_emptyContent_createsEmptyPage() {
        Pageable pageable = PageRequest.of(2, 5);
        PageImplJacksonSerializer<String> page = new PageImplJacksonSerializer<>(new ArrayList<>(), pageable, 0);

        assertTrue(page.getContent().isEmpty());
        assertEquals(0, page.getTotalElements());
        assertEquals(2, page.getNumber());
    }
}
