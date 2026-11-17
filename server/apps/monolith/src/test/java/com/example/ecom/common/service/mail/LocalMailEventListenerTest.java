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
class LocalMailEventListenerTest {

    @Mock
    private MailService mailService;

    @InjectMocks
    private LocalMailEventListener listener;

    @Test
    @DisplayName("onMailEvent should delegate to mailService.sendEmailAsync")
    void onMailEvent_delegatesToMailService() {
        MailMessageDto mail = new MailMessageDto("test@example.com", "Subject", "Body");

        listener.onMailEvent(mail);

        verify(mailService).sendEmailAsync("test@example.com", "Subject", "Body");
    }
}
