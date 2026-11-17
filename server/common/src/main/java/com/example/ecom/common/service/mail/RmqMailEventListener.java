package com.example.ecom.common.service.mail;

import com.example.ecom.common.dto.MailMessageDto;
import com.example.ecom.common.event.DomainEvent;
import com.example.ecom.common.event.DomainEventConfig;
import com.example.ecom.common.event.EventConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnBean(DomainEventConfig.class)
@RequiredArgsConstructor
public class RmqMailEventListener {

    private final MailService mailService;

    @RabbitListener(queues = EventConstants.MAIL_QUEUE)
    public void onMessage(DomainEvent<MailMessageDto> event) {
        MailMessageDto mail = event.payload();
        log.info("Received mail event [eventId={}] for {}", event.eventId(), mail.to());

        mailService.sendEmailAsync(mail.to(), mail.subject(), mail.body());
    }
}
