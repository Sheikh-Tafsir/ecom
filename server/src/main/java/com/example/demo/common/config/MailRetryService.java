package com.example.demo.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailRetryService {

    private final JavaMailSender mailSender;

    @Retryable(retryFor = MailException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendEmail(String recipientEmail, String subject, String body) {

        log.info("Sending email to {}", recipientEmail);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);

        log.info("Email sent successfully to {}", recipientEmail);
    }

    @SuppressWarnings("unused")
    @Recover
    public void recoverSendEmail(MailException e, String recipientEmail, String subject, String body) {
        log.error("Email permanently failed after retries. recipient={}", recipientEmail, e);
    }
}
