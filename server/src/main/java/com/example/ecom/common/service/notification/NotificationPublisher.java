package com.example.ecom.common.service.notification;

@FunctionalInterface
public interface NotificationPublisher {
    void publish(String event);
}
