package com.example.ecom.common.event;

/**
 * Transport-agnostic interface for publishing domain events.
 * <p>
 * Implementations resolve the underlying destination (e.g., RabbitMQ exchange/routing key,
 * Kafka topic, Redis stream) from the {@code eventType}.
 *
 * @see RMqEventPublisher
 * @see RedisEventPublisher
 */
public interface EventPublisher {

    /**
     * Publishes a domain event after the current transaction commits.
     *
     * @param source    the publishing service name (e.g., {@code "order-service"})
     * @param eventType the event type identifier (e.g., {@code "order.stock-release-requested"})
     * @param payload   the event-specific data
     */
    <T> void publish(String source, String eventType, T payload);
}
