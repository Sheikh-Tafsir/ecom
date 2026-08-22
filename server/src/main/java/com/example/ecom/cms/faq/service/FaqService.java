package com.example.ecom.cms.faq.service;

import com.example.ecom.cms.faq.dto.FaqRequest;
import com.example.ecom.cms.faq.dto.FaqResponse;
import com.example.ecom.common.model.Faq;
import com.example.ecom.cms.faq.repository.FaqRepository;
import com.example.ecom.common.service.MessageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.example.ecom.common.utils.CacheConstants.CACHE_FAQS;

@Service
@RequiredArgsConstructor
public class FaqService {

    private final FaqRepository faqRepository;

    private final MessageService messageService;

    @Cacheable(value = CACHE_FAQS)
    public List<FaqResponse> findAll() {
        return faqRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(FaqResponse::new)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyAuthority(T(com.example.ecom.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Transactional
    @CacheEvict(value = CACHE_FAQS, allEntries = true)
    public FaqResponse create(FaqRequest request) {
        Faq faq = new Faq();
        updateFaqFromRequest(faq, request);

        if (faq.getDisplayOrder() == 0) {
            faq.setDisplayOrder(faqRepository.findMaxDisplayOrder().orElse(0) + 1);
        }

        return new FaqResponse(faqRepository.save(faq));
    }

    @PreAuthorize("hasAnyAuthority(T(com.example.ecom.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Transactional
    @CacheEvict(value = CACHE_FAQS, allEntries = true)
    public FaqResponse update(Long id, FaqRequest request) {
        Faq faq = findByIdHelper(id);
        updateFaqFromRequest(faq, request);
        return new FaqResponse(faqRepository.save(faq));
    }

    @PreAuthorize("hasAnyAuthority(T(com.example.ecom.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Transactional
    @CacheEvict(value = CACHE_FAQS, allEntries = true)
    public void delete(Long id) {
        Faq faq = findByIdHelper(id);
        faqRepository.delete(faq);
    }

    private Faq findByIdHelper(Long id) {
        return faqRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "Faq", id)));
    }

    private void updateFaqFromRequest(Faq faq, FaqRequest request) {
        faq.setQuestion(request.getQuestion());
        faq.setAnswer(request.getAnswer());
        faq.setDisplayOrder(request.getDisplayOrder());
    }
}
