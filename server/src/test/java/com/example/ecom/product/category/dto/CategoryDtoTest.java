package com.example.ecom.product.category.dto;

import com.example.ecom.product.category.model.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CategoryDtoTest {

    @Nested
    @DisplayName("CategoryResponse Tests")
    class CategoryResponseTest {

        @Test
        @DisplayName("Canonical constructor sets id and name")
        void constructor_canonicalArgs_setsFields() {
            UUID id = UUID.fromString("01927d72-097f-7afa-9ae0-293041526374");
            CategoryResponse response = new CategoryResponse(id, "Electronics");

            assertEquals(id, response.id());
            assertEquals("Electronics", response.name());
        }

        @Test
        @DisplayName("Construction from Category entity maps fields")
        void constructor_fromEntity_setsFields() {
            UUID id = UUID.fromString("01927d72-1a8f-7bfa-8ae1-304152637485");
            Category category = new Category();
            category.setId(id);
            category.setName("Clothing");

            CategoryResponse response = new CategoryResponse(category);

            assertEquals(id, response.id());
            assertEquals("Clothing", response.name());
        }

        @Test
        @DisplayName("Two equal records are equal")
        void equals_sameFields_returnsTrue() {
            UUID id = UUID.fromString("01927d72-2b9f-7cfa-7ae2-415263748596");
            CategoryResponse r1 = new CategoryResponse(id, "Books");
            CategoryResponse r2 = new CategoryResponse(id, "Books");

            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
        }
    }

    @Nested
    @DisplayName("CategorySaveRequest Tests")
    class CategorySaveRequestTest {

        @Test
        @DisplayName("Record constructor sets name")
        void constructor_validArgs_setsFields() {
            CategorySaveRequest request = new CategorySaveRequest("Sports");
            assertEquals("Sports", request.name());
        }
    }
}
