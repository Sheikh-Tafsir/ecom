package com.example.demo.cms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FaqRequest {

    @NotBlank
    private String question;

    @NotBlank
    private String answer;

    private int displayOrder;
}
