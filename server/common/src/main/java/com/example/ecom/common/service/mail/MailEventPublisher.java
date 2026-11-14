package com.example.ecom.common.service.mail;

import com.example.ecom.common.dto.MailMessageDto;

@FunctionalInterface
public interface MailEventPublisher {

    void send(MailMessageDto message);
}
