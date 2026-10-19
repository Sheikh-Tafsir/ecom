package com.example.ecom.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static com.example.ecom.notification.service.NotificationService.NOTIFICATION_CHANNEL;

/**
 * Publishes notification events to a RabbitMQ fanout exchange.
 * Activated when {@code notification.publisher=amqp} is set and a {@link RabbitTemplate} bean exists.
 */
@Component
@ConditionalOnBean(RabbitTemplate.class)
@ConditionalOnProperty(name = "notification.publisher", havingValue = "amqp")
@RequiredArgsConstructor
public class AmqpNotificationPublisher implements NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(String event) {
        rabbitTemplate.convertAndSend(NOTIFICATION_CHANNEL, "", event);
    }
}
