package com.example.ecom.cms.faq.validator;

import com.example.ecom.cms.faq.dto.FaqRequest;
import com.example.ecom.cms.faq.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.UUID;

@Component
@RefreshScope
@RequiredArgsConstructor
public class FaqValidator {

    @Value("${app.cms.max-total-faqs:10}")
    private int maxTotalFaqs;

    private final FaqRepository faqRepository;

    public void validateCreate(FaqRequest request, Errors errors) {
        if (faqRepository.count() >= maxTotalFaqs) {
            errors.reject("error.limit.exceeded", new Object[]{"FAQs", maxTotalFaqs}, "Total FAQs limit exceeded, cannot add more than " + maxTotalFaqs);
        }

        if (request.getDisplayOrder() != 0 && faqRepository.existsByDisplayOrder(request.getDisplayOrder())) {
            errors.rejectValue("displayOrder", "error.field.duplicate", new Object[]{"Display Order"}, "Display order already exists");
        }
    }

    public void validateUpdate(UUID id, FaqRequest request, Errors errors) {
        if (request.getDisplayOrder() != 0 && faqRepository.existsByDisplayOrderAndIdNot(request.getDisplayOrder(), id)) {
            errors.rejectValue("displayOrder", "error.field.duplicate", new Object[]{"Display Order"}, "Display order already exists");
        }
    }
}
