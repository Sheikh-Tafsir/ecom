package com.example.ecom.common.event;

/**
 * Thrown when an event cannot be published to the message broker.
 * <p>
 * Callers that must not silently lose events (e.g., payment callbacks)
 * should catch this and take compensating action.
 */
public class EventPublishException extends RuntimeException {

    public EventPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
