package com.example.demo.common.service;

import com.example.demo.common.config.MailRetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MailService {

    private final MailRetryService mailRetryService;

    @Async
    public void sendEmailAsync(String recipientEmail, String subject, String body) {
        mailRetryService.sendEmail(recipientEmail, subject, body);
    }
}
