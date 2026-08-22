package com.example.ecom.cms.faq.dto;

import com.example.ecom.common.model.Faq;
import lombok.Data;

@Data
public class FaqResponse {

    private Long id;

    private String question;

    private String answer;

    private int displayOrder;

    public FaqResponse(Faq faq) {
        this.id = faq.getId();
        this.question = faq.getQuestion();
        this.answer = faq.getAnswer();
        this.displayOrder = faq.getDisplayOrder();
    }
}
