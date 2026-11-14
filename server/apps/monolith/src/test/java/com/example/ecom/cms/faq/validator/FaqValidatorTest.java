package com.example.ecom.cms.faq.validator;

import com.example.ecom.cms.faq.dto.FaqRequest;
import com.example.ecom.cms.faq.repository.FaqRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaqValidatorTest {

    @Mock
    private FaqRepository faqRepository;

    @InjectMocks
    private FaqValidator faqValidator;

    private FaqRequest faqRequest;
    private Errors errors;

    @BeforeEach
    void setUp() throws Exception {
        Field maxTotalField = FaqValidator.class.getDeclaredField("maxTotalFaqs");
        maxTotalField.setAccessible(true);
        maxTotalField.setInt(faqValidator, 10);
        faqRequest = new FaqRequest();
        faqRequest.setQuestion("How do I track my order?");
        faqRequest.setAnswer("You can track your order using the tracking link in your email.");
        faqRequest.setDisplayOrder(5);
        errors = new BeanPropertyBindingResult(faqRequest, "faqRequest");
    }

    // ── validateCreate ──────────────────────────────────────────

    @Test
    @DisplayName("validateCreate should pass when all validations are satisfied")
    void validateCreate_validRequest_noErrors() {
        when(faqRepository.count()).thenReturn(5L);
        when(faqRepository.existsByDisplayOrder(5)).thenReturn(false);

        faqValidator.validateCreate(faqRequest, errors);

        assertFalse(errors.hasErrors());
        verify(faqRepository).count();
        verify(faqRepository).existsByDisplayOrder(5);
    }

    @Test
    @DisplayName("validateCreate should reject when total FAQ limit is exceeded")
    void validateCreate_totalLimitExceeded_rejectsGlobal() {
        when(faqRepository.count()).thenReturn(10L);
        when(faqRepository.existsByDisplayOrder(5)).thenReturn(false);

        faqValidator.validateCreate(faqRequest, errors);

        assertTrue(errors.hasGlobalErrors());
        assertEquals("error.limit.exceeded", errors.getGlobalErrors().get(0).getCode());
        verify(faqRepository).count();
    }

    @Test
    @DisplayName("validateCreate should reject when display order already exists")
    void validateCreate_duplicateDisplayOrder_rejectsDisplayOrderField() {
        when(faqRepository.count()).thenReturn(5L);
        when(faqRepository.existsByDisplayOrder(5)).thenReturn(true);

        faqValidator.validateCreate(faqRequest, errors);

        assertTrue(errors.hasFieldErrors("displayOrder"));
        assertEquals("error.field.duplicate", errors.getFieldError("displayOrder").getCode());
        verify(faqRepository).existsByDisplayOrder(5);
    }

    @Test
    @DisplayName("validateCreate should skip display order check when order is zero")
    void validateCreate_displayOrderZero_skipsDisplayOrderCheck() {
        faqRequest.setDisplayOrder(0);
        when(faqRepository.count()).thenReturn(5L);

        faqValidator.validateCreate(faqRequest, errors);

        assertFalse(errors.hasFieldErrors("displayOrder"));
        verify(faqRepository, never()).existsByDisplayOrder(anyInt());
    }

    @Test
    @DisplayName("validateCreate should report both limit exceeded and duplicate display order")
    void validateCreate_multipleViolations_reportsAllErrors() {
        when(faqRepository.count()).thenReturn(10L);
        when(faqRepository.existsByDisplayOrder(5)).thenReturn(true);

        faqValidator.validateCreate(faqRequest, errors);

        assertTrue(errors.hasGlobalErrors());
        assertTrue(errors.hasFieldErrors("displayOrder"));
    }

    // ── validateUpdate ──────────────────────────────────────────

    @Test
    @DisplayName("validateUpdate should pass when display order is unique for different id")
    void validateUpdate_uniqueDisplayOrder_noErrors() {
        UUID faqId = UUID.fromString("f6a7b8c9-d0e1-2345-fabc-456789012345");
        when(faqRepository.existsByDisplayOrderAndIdNot(5, faqId)).thenReturn(false);

        faqValidator.validateUpdate(faqId, faqRequest, errors);

        assertFalse(errors.hasErrors());
        verify(faqRepository).existsByDisplayOrderAndIdNot(5, faqId);
    }

    @Test
    @DisplayName("validateUpdate should reject when display order conflicts with another FAQ")
    void validateUpdate_duplicateDisplayOrder_rejectsDisplayOrderField() {
        UUID faqId = UUID.fromString("f6a7b8c9-d0e1-2345-fabc-456789012345");
        when(faqRepository.existsByDisplayOrderAndIdNot(5, faqId)).thenReturn(true);

        faqValidator.validateUpdate(faqId, faqRequest, errors);

        assertTrue(errors.hasFieldErrors("displayOrder"));
        assertEquals("error.field.duplicate", errors.getFieldError("displayOrder").getCode());
        verify(faqRepository).existsByDisplayOrderAndIdNot(5, faqId);
    }

    @Test
    @DisplayName("validateUpdate should skip display order check when order is zero")
    void validateUpdate_displayOrderZero_skipsDisplayOrderCheck() {
        UUID faqId = UUID.fromString("f6a7b8c9-d0e1-2345-fabc-456789012345");
        faqRequest.setDisplayOrder(0);

        faqValidator.validateUpdate(faqId, faqRequest, errors);

        assertFalse(errors.hasErrors());
        verify(faqRepository, never()).existsByDisplayOrderAndIdNot(anyInt(), any());
    }
}
