package com.example.ecom.order.event;

import com.example.ecom.common.event.DomainEvent;
import com.example.ecom.common.event.DomainEventConfig;
import com.example.ecom.common.event.EventConstants;
import com.example.ecom.common.event.PaymentCompletedEvent;
import com.example.ecom.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ infrastructure adapter for payment-completed events.
 * <p>
 * Listens to the AMQP queue and delegates directly to {@link OrderService#acceptOrderForPrepayment}.
 * Re-throws exceptions on unhandled failures so Spring AMQP routes the message to the DLQ.
 */
@Slf4j
@Component
@ConditionalOnBean(DomainEventConfig.class)
@RequiredArgsConstructor
public class RmqPaymentCompletedListener {

    private final OrderService orderService;

    @RabbitListener(queues = EventConstants.ORDER_PAYMENT_EVENTS_QUEUE)
    public void onMessage(DomainEvent<PaymentCompletedEvent> event) {
        log.info("Received {} from RabbitMQ queue {} [eventId={}]",
                event.type(), EventConstants.ORDER_PAYMENT_EVENTS_QUEUE, event.eventId());
        try {
            orderService.acceptOrderForPrepayment(event.payload().orderId());
        } catch (Exception e) {
            log.error("Failed to process payment completed event for order {} (eventId={}): {}",
                    event.payload().orderId(), event.eventId(), e.getMessage(), e);
            throw e; // re-throw so RabbitMQ routes to DLQ
        }
    }
}
