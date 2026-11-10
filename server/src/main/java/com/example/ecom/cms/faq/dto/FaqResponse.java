package com.example.ecom.cms.faq.dto;

import com.example.ecom.cms.faq.model.Faq;

import java.util.UUID;

public record FaqResponse(UUID id, String question, String answer, int displayOrder) {

    public FaqResponse(Faq faq) {
        this(faq.getId(), faq.getQuestion(), faq.getAnswer(), faq.getDisplayOrder());
    }
}
