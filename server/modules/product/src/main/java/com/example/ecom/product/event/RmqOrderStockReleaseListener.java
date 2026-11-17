package com.example.ecom.product.event;

import com.example.ecom.common.event.DomainEvent;
import com.example.ecom.common.event.DomainEventConfig;
import com.example.ecom.common.event.EventConstants;
import com.example.ecom.common.event.OrderStockReleaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ infrastructure adapter for order stock-release events.
 * <p>
 * Listens to the AMQP queue and delegates directly to {@link OrderStockReleaseService},
 * passing the {@code eventId} for consumer deduplication.
 */
@Slf4j
@Component
@ConditionalOnBean(DomainEventConfig.class)
@RequiredArgsConstructor
public class RmqOrderStockReleaseListener {

    private final OrderStockReleaseService stockReleaseService;

    @RabbitListener(queues = EventConstants.PRODUCT_ORDER_EVENTS_QUEUE)
    public void onMessage(DomainEvent<OrderStockReleaseEvent> event) {
        log.info("Received {} from RabbitMQ queue {} [eventId={}]",
                event.type(), EventConstants.PRODUCT_ORDER_EVENTS_QUEUE, event.eventId());
        stockReleaseService.releaseStock(event.eventId(), event.payload());
    }
}
