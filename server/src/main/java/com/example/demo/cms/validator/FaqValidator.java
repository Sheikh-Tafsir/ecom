package com.example.demo.cms.validator;

import com.example.demo.cms.dto.FaqRequest;
import com.example.demo.cms.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

@Component
@RequiredArgsConstructor
public class FaqValidator {

    public static final int MAX_TOTAL_FAQS = 10;

    private final FaqRepository faqRepository;

    public void validateCreate(FaqRequest request, Errors errors) {
        if (faqRepository.count() >= MAX_TOTAL_FAQS) {
            errors.reject("error.limit.exceeded", new Object[]{"FAQs", MAX_TOTAL_FAQS}, "Total FAQs limit exceeded, cannot add more than " + MAX_TOTAL_FAQS);
        }

        if (request.getDisplayOrder() != 0 && faqRepository.existsByDisplayOrder(request.getDisplayOrder())) {
            errors.rejectValue("displayOrder", "error.field.duplicate", new Object[]{"Display Order"}, "Display order already exists");
        }
    }

    public void validateUpdate(Long id, FaqRequest request, Errors errors) {
        if (request.getDisplayOrder() != 0 && faqRepository.existsByDisplayOrderAndIdNot(request.getDisplayOrder(), id)) {
            errors.rejectValue("displayOrder", "error.field.duplicate", new Object[]{"Display Order"}, "Display order already exists");
        }
    }
}
