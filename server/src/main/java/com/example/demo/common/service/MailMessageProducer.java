package com.example.demo.common.service;

import com.example.demo.common.dto.MailMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MailMessageProducer {

    @Value("${mail.exchange}")
    private String mailExchange;

    @Value("${mail.routing-key}")
    private String mailRoutingKey;

    private final RabbitTemplate rabbitTemplate;

    public void sendMail(MailMessageDto mailMessageDto) {
        rabbitTemplate.convertAndSend(
                mailExchange,
                mailRoutingKey,
                mailMessageDto
        );
    }
}