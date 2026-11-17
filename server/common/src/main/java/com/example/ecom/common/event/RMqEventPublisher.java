package com.example.ecom.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * RabbitMQ implementation of {@link EventPublisher}.
 * <p>
 * Publishes domain events to RabbitMQ <em>after</em> the current transaction commits.
 * The exchange is resolved automatically from the event type via {@link EventConstants#exchangeFor(String)}.
 * <p>
 * If called outside a transaction, the event is published immediately.
 * <p>
 * Every event is wrapped in a {@link DomainEvent} envelope with a unique {@code eventId}
 * for consumer-side deduplication.
 */
@Slf4j
@Primary
@Component
@ConditionalOnBean(RabbitTemplate.class)
@RequiredArgsConstructor
public class RMqEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public <T> void publish(String source, String eventType, T payload) {
        String exchange = EventConstants.exchangeFor(eventType);
        DomainEvent<T> event = DomainEvent.of(eventType, source, payload);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            doPublish(exchange, eventType, event);
                        }
                    }
            );
        } else {
            doPublish(exchange, eventType, event);
        }
    }

    private void doPublish(String exchange, String routingKey, DomainEvent<?> event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("Published event {} [{}] to {}/{}",
                    event.eventId(), event.type(), exchange, routingKey);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to publish event {} [{}] to {}/{}. " +
                    "Reconciliation may be needed.", event.eventId(), event.type(),
                    exchange, routingKey, e);
            throw new EventPublishException("Failed to publish event " + event.eventId()
                    + " [" + event.type() + "] to " + exchange + "/" + routingKey, e);
        }
    }
}
