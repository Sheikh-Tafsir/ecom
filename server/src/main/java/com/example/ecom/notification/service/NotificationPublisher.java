package com.example.ecom.notification.service;

@FunctionalInterface
public interface NotificationPublisher {
    void publish(String event);
}
