package com.example.ecom.user.user.service;

import java.util.UUID;

@FunctionalInterface
public interface UserEventPublisher {

    void publishUserChanged(UUID userId);
}
