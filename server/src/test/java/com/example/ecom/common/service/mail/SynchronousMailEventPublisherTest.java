package com.example.ecom.common.service.mail;

import com.example.ecom.common.dto.MailMessageDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SynchronousMailEventPublisherTest {

    @Mock
    private MailService mailService;

    @InjectMocks
    private SynchronousMailEventPublisher synchronousMailEventPublisher;

    @Test
    @DisplayName("send should call MailService.sendEmailAsync directly, bypassing MQ")
    void send_callsMailServiceDirectly() {
        MailMessageDto dto = new MailMessageDto("user@test.com", "Reset Password", "Click here");

        synchronousMailEventPublisher.send(dto);

        verify(mailService).sendEmailAsync("user@test.com", "Reset Password", "Click here");
    }
}
