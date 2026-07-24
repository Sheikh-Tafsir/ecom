package com.example.demo.cms.controller;

import com.example.demo.cms.dto.FaqRequest;
import com.example.demo.cms.dto.FaqResponse;
import com.example.demo.cms.service.FaqService;
import com.example.demo.cms.validator.FaqValidator;
import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.service.MessageService;
import com.example.demo.common.utils.ResponseUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.demo.common.utils.Utils.checkErrors;

@RestController
@RequestMapping("/faqs")
@RequiredArgsConstructor
public class FaqController {

    private final FaqService faqService;

    private final FaqValidator faqValidator;

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FaqResponse>>> findAll() {
        List<FaqResponse> faqs = faqService.findAll();
        return ResponseUtils.ok(faqs, messageService.get("successfully.found", "FAQ List"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FaqResponse>> create(@Valid @RequestBody FaqRequest request,
                                                           BindingResult bindingResult) {

        faqValidator.validateCreate(request, bindingResult);
        checkErrors(bindingResult);

        FaqResponse faq = faqService.create(request);
        return ResponseUtils.created(faq, messageService.get("successfully.created", "FAQ"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FaqResponse>> update(@PathVariable Long id, @Valid @RequestBody FaqRequest request,
                                                           BindingResult bindingResult) {

        faqValidator.validateUpdate(id, request, bindingResult);
        checkErrors(bindingResult);

        FaqResponse faq = faqService.update(id, request);
        return ResponseUtils.ok(faq, messageService.get("successfully.updated", "FAQ"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        faqService.delete(id);
        return ResponseUtils.ok(messageService.get("successfully.deleted", "FAQ"));
    }
}
