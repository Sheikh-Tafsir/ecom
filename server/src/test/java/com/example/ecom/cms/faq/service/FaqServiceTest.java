package com.example.ecom.cms.faq.service;

import com.example.ecom.cms.faq.dto.FaqRequest;
import com.example.ecom.cms.faq.dto.FaqResponse;
import com.example.ecom.cms.faq.repository.FaqRepository;
import com.example.ecom.common.model.Faq;
import com.example.ecom.common.service.MessageService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaqServiceTest {

    @Mock
    private FaqRepository faqRepository;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private FaqService faqService;

    private UUID faqId;
    private Faq faq;
    private FaqRequest faqRequest;

    @BeforeEach
    void setUp() {
        faqId = UUID.fromString("e5f6a7b8-c9d0-1234-efab-345678901234");

        faq = new Faq();
        faq.setId(faqId);
        faq.setQuestion("What is your return policy?");
        faq.setAnswer("We accept returns within 30 days of purchase.");
        faq.setDisplayOrder(1);

        faqRequest = new FaqRequest();
        faqRequest.setQuestion("What is your return policy?");
        faqRequest.setAnswer("We accept returns within 30 days of purchase.");
        faqRequest.setDisplayOrder(1);
    }

    // ── findAll ─────────────────────────────────────────────────

    @Test
    @DisplayName("findAll should return all FAQs ordered by display order")
    void findAll_faqsExist_returnsFaqResponseList() {
        Faq faq2 = new Faq();
        faq2.setId(UUID.randomUUID());
        faq2.setQuestion("How long does shipping take?");
        faq2.setAnswer("Standard shipping takes 5-7 business days.");
        faq2.setDisplayOrder(2);

        when(faqRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(faq, faq2));

        List<FaqResponse> result = faqService.findAll();

        assertEquals(2, result.size());
        assertEquals("What is your return policy?", result.get(0).question());
        assertEquals("How long does shipping take?", result.get(1).question());
        verify(faqRepository).findAllByOrderByDisplayOrderAsc();
    }

    @Test
    @DisplayName("findAll should return empty list when no FAQs exist")
    void findAll_noFaqs_returnsEmptyList() {
        when(faqRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(Collections.emptyList());

        List<FaqResponse> result = faqService.findAll();

        assertTrue(result.isEmpty());
        verify(faqRepository).findAllByOrderByDisplayOrderAsc();
    }

    // ── create ──────────────────────────────────────────────────

    @Test
    @DisplayName("create should persist FAQ with specified display order and return response")
    void create_withDisplayOrder_savesFaqAndReturnsResponse() {
        when(faqRepository.save(any(Faq.class))).thenReturn(faq);

        FaqResponse result = faqService.create(faqRequest);

        assertNotNull(result);
        assertEquals("What is your return policy?", result.question());
        assertEquals("We accept returns within 30 days of purchase.", result.answer());
        assertEquals(1, result.displayOrder());
        verify(faqRepository).save(any(Faq.class));
        verify(faqRepository, never()).findMaxDisplayOrder();
    }

    @Test
    @DisplayName("create should auto-assign display order when it is zero")
    void create_withZeroDisplayOrder_autoAssignsDisplayOrder() {
        faqRequest.setDisplayOrder(0);
        Faq savedFaq = new Faq();
        savedFaq.setId(faqId);
        savedFaq.setQuestion("What is your return policy?");
        savedFaq.setAnswer("We accept returns within 30 days of purchase.");
        savedFaq.setDisplayOrder(4);

        when(faqRepository.findMaxDisplayOrder()).thenReturn(Optional.of(3));
        when(faqRepository.save(any(Faq.class))).thenReturn(savedFaq);

        FaqResponse result = faqService.create(faqRequest);

        assertNotNull(result);
        verify(faqRepository).findMaxDisplayOrder();
        verify(faqRepository).save(any(Faq.class));
    }

    @Test
    @DisplayName("create should set display order to 1 when no FAQs exist and order is zero")
    void create_withZeroDisplayOrderAndNoFaqs_setsDisplayOrderToOne() {
        faqRequest.setDisplayOrder(0);
        Faq savedFaq = new Faq();
        savedFaq.setId(faqId);
        savedFaq.setQuestion("What is your return policy?");
        savedFaq.setAnswer("We accept returns within 30 days of purchase.");
        savedFaq.setDisplayOrder(1);

        when(faqRepository.findMaxDisplayOrder()).thenReturn(Optional.empty());
        when(faqRepository.save(any(Faq.class))).thenReturn(savedFaq);

        FaqResponse result = faqService.create(faqRequest);

        assertNotNull(result);
        verify(faqRepository).findMaxDisplayOrder();
        verify(faqRepository).save(any(Faq.class));
    }

    // ── update ──────────────────────────────────────────────────

    @Test
    @DisplayName("update should modify existing FAQ and return updated response")
    void update_existingFaq_updatesAndReturnsResponse() {
        faqRequest.setAnswer("Updated return policy: 60 days.");
        Faq updatedFaq = new Faq();
        updatedFaq.setId(faqId);
        updatedFaq.setQuestion("What is your return policy?");
        updatedFaq.setAnswer("Updated return policy: 60 days.");
        updatedFaq.setDisplayOrder(1);

        when(faqRepository.findById(faqId)).thenReturn(Optional.of(faq));
        when(faqRepository.save(any(Faq.class))).thenReturn(updatedFaq);

        FaqResponse result = faqService.update(faqId, faqRequest);

        assertNotNull(result);
        assertEquals("Updated return policy: 60 days.", result.answer());
        verify(faqRepository).findById(faqId);
        verify(faqRepository).save(any(Faq.class));
    }

    @Test
    @DisplayName("update should throw EntityNotFoundException when FAQ not found")
    void update_nonExistentFaq_throwsEntityNotFoundException() {
        UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000005");
        when(faqRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        when(messageService.get(eq("error.entity.not.found"), eq("Faq"), eq(nonExistentId)))
                .thenReturn("Faq not found with id: " + nonExistentId);

        assertThrows(EntityNotFoundException.class, () -> faqService.update(nonExistentId, faqRequest));

        verify(faqRepository).findById(nonExistentId);
        verify(faqRepository, never()).save(any());
    }

    // ── delete ──────────────────────────────────────────────────

    @Test
    @DisplayName("delete should remove existing FAQ")
    void delete_existingFaq_deletesFaq() {
        when(faqRepository.findById(faqId)).thenReturn(Optional.of(faq));

        faqService.delete(faqId);

        verify(faqRepository).findById(faqId);
        verify(faqRepository).delete(faq);
    }

    @Test
    @DisplayName("delete should throw EntityNotFoundException when FAQ not found")
    void delete_nonExistentFaq_throwsEntityNotFoundException() {
        UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000006");
        when(faqRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        when(messageService.get(eq("error.entity.not.found"), eq("Faq"), eq(nonExistentId)))
                .thenReturn("Faq not found with id: " + nonExistentId);

        assertThrows(EntityNotFoundException.class, () -> faqService.delete(nonExistentId));

        verify(faqRepository).findById(nonExistentId);
        verify(faqRepository, never()).delete(any());
    }

    @Test
    @DisplayName("create should correctly map all request fields to FAQ entity")
    void create_withAllFields_mapsAllFieldsCorrectly() {
        when(faqRepository.save(any(Faq.class))).thenAnswer(invocation -> {
            Faq saved = invocation.getArgument(0);
            saved.setId(faqId);
            assertEquals("What is your return policy?", saved.getQuestion());
            assertEquals("We accept returns within 30 days of purchase.", saved.getAnswer());
            assertEquals(1, saved.getDisplayOrder());
            return saved;
        });

        faqService.create(faqRequest);

        verify(faqRepository).save(any(Faq.class));
    }
}
