package com.example.ecom.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base envelope for all domain events published across service boundaries.
 * <p>
 * Every event carries a unique {@code eventId} for idempotent consumer deduplication
 * and a {@code timestamp} for ordering and debugging.
 *
 * @param eventId   unique identifier for this event instance (used for deduplication)
 * @param type      event type (e.g., "order.stock-release-requested")
 * @param source    the service that published this event
 * @param timestamp when the event was created
 * @param payload   the event-specific data
 */
public record DomainEvent<T>(
        UUID eventId,
        String type,
        String source,
        Instant timestamp,
        T payload
) {
    public static <T> DomainEvent<T> of(String type, String source, T payload) {
        return new DomainEvent<>(UUID.randomUUID(), type, source, Instant.now(), payload);
    }
}
