package com.example.ecom.common.service.mail;

import com.example.ecom.common.dto.MailMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnBean(RabbitTemplate.class)
@RequiredArgsConstructor
public class RabbitMailEventConsumer {

    private final MailService mailService;

    @RabbitListener(queues = "${spring.rabbitmq.mail.queue:mail.queue}")
    public void handleMail(MailMessageDto mailMessageDto) {
        mailService.sendEmailAsync(mailMessageDto.to(), mailMessageDto.subject(), mailMessageDto.body());

        log.info("Mail sent to {}", mailMessageDto.to());
    }
}