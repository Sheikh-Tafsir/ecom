package com.example.ecom.common.service.mail;

import com.example.ecom.common.dto.MailMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalMailEventListener {

    private final MailService mailService;

    @EventListener
    public void onMailEvent(MailMessageDto mail) {
        log.info("Sending mail locally (no MQ) to {}", mail.to());
        mailService.sendEmailAsync(mail.to(), mail.subject(), mail.body());
    }
}
