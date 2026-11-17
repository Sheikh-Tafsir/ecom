package com.example.ecom.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * In-process fallback when RabbitMQ is not available.
 * <p>
 * When running in monolith mode without a message broker, this publisher dispatches
 * domain events in-memory via Spring's standard {@link ApplicationEventPublisher}.
 * Any component with {@code @EventListener} for that event payload type will be invoked locally.
 */
@Slf4j
@Component
@ConditionalOnMissingBean(RMqEventPublisher.class)
@RequiredArgsConstructor
public class LocalEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public <T> void publish(String source, String eventType, T payload) {
        log.info("Dispatching event [{}] from [{}] locally via Spring ApplicationEventPublisher",
                eventType, source);
        applicationEventPublisher.publishEvent(payload);
    }
}
