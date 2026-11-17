package com.example.ecom.product.event;

import com.example.ecom.common.event.OrderStockReleaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens to in-memory Spring events when running in monolith / local mode without RabbitMQ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalOrderStockReleaseListener {

    private final OrderStockReleaseService stockReleaseService;

    @EventListener
    public void onOrderStockRelease(OrderStockReleaseEvent event) {
        log.info("Received local Spring event for order stock release {}", event.orderId());
        stockReleaseService.releaseStock(event);
    }
}
