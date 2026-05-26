package com.example.demo.common.service;

import com.example.demo.common.dto.MailMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailMessageConsumer {

    private final MailService mailService;

    @RabbitListener(queues = "${mail.queue}")
    public void handleMail(MailMessageDto mailMessageDto) {
        mailService.sendEmail(mailMessageDto.to(), mailMessageDto.subject(), mailMessageDto.body());

        log.info("Mail sent to {}", mailMessageDto.to());
    }
}