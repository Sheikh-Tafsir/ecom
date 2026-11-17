package com.example.ecom.common.service.mail;

import com.example.ecom.common.dto.MailMessageDto;
import com.example.ecom.common.event.DomainEvent;
import com.example.ecom.common.event.EventConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RmqMailEventListenerTest {

    @Mock
    private MailService mailService;

    @InjectMocks
    private RmqMailEventListener listener;

    @Test
    @DisplayName("onMessage should call MailService.sendEmailAsync with event payload")
    void onMessage_callsMailService() {
        MailMessageDto mail = new MailMessageDto("user@test.com", "Welcome", "Hello!");
        DomainEvent<MailMessageDto> event = new DomainEvent<>(
                UUID.randomUUID(), EventConstants.MAIL_SEND, EventConstants.SOURCE_AUTH,
                java.time.Instant.now(), mail
        );

        listener.onMessage(event);

        verify(mailService).sendEmailAsync("user@test.com", "Welcome", "Hello!");
    }
}
