package com.example.ecom.common.service.mail;

import com.example.ecom.common.dto.MailMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(RabbitTemplate.class)
@RequiredArgsConstructor
public class RabbitMailEventPublisher implements MailEventPublisher {

    @Value("${spring.rabbitmq.mail.exchange}")
    private String mailExchange;

    @Value("${spring.rabbitmq.mail.routing-key}")
    private String mailRoutingKey;

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void send(MailMessageDto mailMessageDto) {
        rabbitTemplate.convertAndSend(
                mailExchange,
                mailRoutingKey,
                mailMessageDto
        );
    }
}