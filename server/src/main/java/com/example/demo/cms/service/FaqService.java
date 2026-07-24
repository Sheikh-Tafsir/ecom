package com.example.demo.cms.service;

import com.example.demo.cms.dto.FaqRequest;
import com.example.demo.cms.dto.FaqResponse;
import com.example.demo.common.model.Faq;
import com.example.demo.cms.repository.FaqRepository;
import com.example.demo.common.service.MessageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FaqService {

    private final FaqRepository faqRepository;

    private final MessageService messageService;

    public List<FaqResponse> findAll() {
        return faqRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(FaqResponse::new)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyAuthority(T(com.example.demo.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Transactional
    public FaqResponse create(FaqRequest request) {
        Faq faq = new Faq();
        updateFaqFromRequest(faq, request);
        
        if (faq.getDisplayOrder() == 0) {
            faq.setDisplayOrder(faqRepository.findMaxDisplayOrder().orElse(0) + 1);
        }
        
        return new FaqResponse(faqRepository.save(faq));
    }

    @PreAuthorize("hasAnyAuthority(T(com.example.demo.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Transactional
    public FaqResponse update(Long id, FaqRequest request) {
        Faq faq = findByIdHelper(id);
        updateFaqFromRequest(faq, request);
        return new FaqResponse(faqRepository.save(faq));
    }

    @PreAuthorize("hasAnyAuthority(T(com.example.demo.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Transactional
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
