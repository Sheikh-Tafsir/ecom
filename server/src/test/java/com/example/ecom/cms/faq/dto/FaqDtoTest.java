package com.example.ecom.cms.faq.dto;

import com.example.ecom.common.model.Faq;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FaqDtoTest {

    @Nested
    @DisplayName("FaqRequest Tests")
    class FaqRequestTest {

        @Test
        @DisplayName("Setters and getters work correctly")
        void settersGetters_allFields_workCorrectly() {
            FaqRequest request = new FaqRequest();
            request.setQuestion("What is the return policy?");
            request.setAnswer("You can return items within 30 days.");
            request.setDisplayOrder(1);

            assertEquals("What is the return policy?", request.getQuestion());
            assertEquals("You can return items within 30 days.", request.getAnswer());
            assertEquals(1, request.getDisplayOrder());
        }
    }

    @Nested
    @DisplayName("FaqResponse Tests")
    class FaqResponseTest {

        @Test
        @DisplayName("Canonical constructor sets all fields")
        void constructor_canonicalArgs_setsFields() {
            UUID id = UUID.fromString("01927d7a-097f-7afa-9af7-637485960718");
            FaqResponse response = new FaqResponse(id, "How to track order?", "Use the orders page.", 2);

            assertEquals(id, response.id());
            assertEquals("How to track order?", response.question());
            assertEquals("Use the orders page.", response.answer());
            assertEquals(2, response.displayOrder());
        }

        @Test
        @DisplayName("Construction from Faq entity maps all fields")
        void constructor_fromEntity_setsFields() {
            UUID id = UUID.fromString("01927d7a-1a8f-7bfa-8af8-748596071829");
            Faq faq = new Faq();
            faq.setId(id);
            faq.setQuestion("Do you offer free shipping?");
            faq.setAnswer("Yes, for orders over 1000 BDT.");
            faq.setDisplayOrder(5);

            FaqResponse response = new FaqResponse(faq);

            assertEquals(id, response.id());
            assertEquals("Do you offer free shipping?", response.question());
            assertEquals("Yes, for orders over 1000 BDT.", response.answer());
            assertEquals(5, response.displayOrder());
        }

        @Test
        @DisplayName("Two equal records are equal")
        void equals_sameFields_returnsTrue() {
            UUID id = UUID.fromString("01927d7a-2b9f-7cfa-7af9-859607182930");
            FaqResponse r1 = new FaqResponse(id, "Q?", "A.", 1);
            FaqResponse r2 = new FaqResponse(id, "Q?", "A.", 1);

            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
        }
    }
}
