package com.example.ecom.common.service.mail;

import com.example.ecom.common.dto.MailMessageDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitMailEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RabbitMailEventPublisher rabbitMailEventPublisher;

    @Test
    @DisplayName("send should publish MailMessageDto to RabbitMQ exchange with routing key")
    void send_publishesToRabbitMQ() {
        ReflectionTestUtils.setField(rabbitMailEventPublisher, "mailExchange", "mail.exchange");
        ReflectionTestUtils.setField(rabbitMailEventPublisher, "mailRoutingKey", "mail.send");

        MailMessageDto dto = new MailMessageDto("user@test.com", "Welcome", "Hello!");

        rabbitMailEventPublisher.send(dto);

        verify(rabbitTemplate).convertAndSend("mail.exchange", "mail.send", dto);
    }
}
