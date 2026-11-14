package com.example.ecom.common.service.mail;

import com.example.ecom.common.dto.MailMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Synchronous fallback implementation of {@link MailEventPublisher}.
 * Bypasses the message queue and calls {@link MailService} directly.
 *
 * <p>Activated automatically when no RabbitMQ-based publisher is available
 * (e.g. local development without a running RabbitMQ instance).</p>
 */
@Slf4j
@Component
@ConditionalOnMissingBean(RabbitMailEventPublisher.class)
@RequiredArgsConstructor
public class SynchronousMailEventPublisher implements MailEventPublisher {

    private final MailService mailService;

    @Override
    public void send(MailMessageDto message) {
        log.info("Sending mail synchronously (no MQ) to {}", message.to());
        mailService.sendEmailAsync(message.to(), message.subject(), message.body());
    }
}
