package com.example.ecom.common.event;

import java.util.Map;

/**
 * Central registry of all domain event exchange names, routing keys, and queue names.
 * <p>
 * Convention:
 * <ul>
 *     <li>Exchange: {@code ecom.<domain>} (topic exchange per bounded context)</li>
 *     <li>Routing key / event type: {@code <domain>.<action>} (dot-separated, past tense)</li>
 *     <li>Queue: {@code <consuming-service>.<domain>-events} (unique per consumer)</li>
 *     <li>DLQ: {@code <queue>.dlq} (dead-letter queue per consumer queue)</li>
 * </ul>
 */
public final class EventConstants {

    private EventConstants() {}

    // ── Exchanges ───────────────────────────────────────────────────
    public static final String ORDER_EXCHANGE = "ecom.orders";
    public static final String PAYMENT_EXCHANGE = "ecom.payments";
    public static final String MAIL_EXCHANGE = "mail.exchange";

    // ── Event types (also used as routing keys in RabbitMQ) ─────────
    public static final String ORDER_STOCK_RELEASE = "order.stock-release-requested";
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String NOTIFICATION = "notification.push";
    public static final String MAIL_SEND = "mail.send";

    // ── Queues ──────────────────────────────────────────────────────
    public static final String PRODUCT_ORDER_EVENTS_QUEUE = "product-svc.order-events";
    public static final String ORDER_PAYMENT_EVENTS_QUEUE = "order-svc.payment-events";
    public static final String MAIL_QUEUE = "mail.queue";

    // ── Dead-letter ─────────────────────────────────────────────────
    public static final String DLX = "ecom.dlx";
    public static final String PRODUCT_ORDER_EVENTS_DLQ = PRODUCT_ORDER_EVENTS_QUEUE + ".dlq";
    public static final String ORDER_PAYMENT_EVENTS_DLQ = ORDER_PAYMENT_EVENTS_QUEUE + ".dlq";

    // ── Redis channels ──────────────────────────────────────────────
    public static final String NOTIFICATION_CHANNEL = "ecom:notifications";
    public static final String REDIS_EVENT_CHANNEL_PREFIX = "ecom:events:";

    // ── Source identifiers ──────────────────────────────────────────
    public static final String SOURCE_ORDER = "order-service";
    public static final String SOURCE_PAYMENT = "payment-service";
    public static final String SOURCE_AUTH = "auth-service";
    public static final String SOURCE_NOTIFICATION = "notification-service";

    // ── Event type → exchange mapping ─────────────────────────────────
    private static final Map<String, String> EVENT_TYPE_TO_EXCHANGE = Map.of(
            ORDER_STOCK_RELEASE, ORDER_EXCHANGE,
            PAYMENT_COMPLETED, PAYMENT_EXCHANGE,
            MAIL_SEND, MAIL_EXCHANGE
    );

    /**
     * Resolves the RabbitMQ exchange for a given event type.
     * <p>
     * Note: {@link #NOTIFICATION} is dispatched exclusively via Redis Pub/Sub
     * (channel {@link #NOTIFICATION_CHANNEL}) to the WebSocket chat-server,
     * so it has no RabbitMQ exchange.
     *
     * @throws IllegalArgumentException if the event type is not registered or is Redis-only
     */
    public static String exchangeFor(String eventType) {
        if (NOTIFICATION.equals(eventType)) {
            throw new IllegalArgumentException(
                    "Event type '" + NOTIFICATION + "' is dispatched exclusively via Redis pub/sub (" +
                    NOTIFICATION_CHANNEL + ") to the WebSocket chat-server, not via RabbitMQ.");
        }
        String exchange = EVENT_TYPE_TO_EXCHANGE.get(eventType);
        if (exchange == null) {
            throw new IllegalArgumentException("No exchange registered for event type: " + eventType);
        }
        return exchange;
    }
}
