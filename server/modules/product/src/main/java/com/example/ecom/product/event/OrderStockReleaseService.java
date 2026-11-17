package com.example.ecom.product.event;

import com.example.ecom.common.event.OrderStockReleaseEvent;
import com.example.ecom.common.service.IdempotencyService;
import com.example.ecom.product.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Domain service for releasing reserved product stock when an order is cancelled or fails.
 * <p>
 * Decoupled from any message broker or event framework — can be invoked directly by tests
 * or by listener adapters ({@link LocalOrderStockReleaseListener}, {@link RmqOrderStockReleaseListener}).
 * Includes idempotency check via {@link IdempotencyService} to prevent duplicate stock releases on redelivered events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStockReleaseService {

    private final ProductService productService;
    private final IdempotencyService idempotencyService;

    public void releaseStock(OrderStockReleaseEvent event) {
        releaseStock(null, event);
    }

    /**
     * Core business logic: restores stock quantities for each item in the cancelled order.
     * Guarded by idempotency check when eventId is provided.
     */
    public void releaseStock(UUID eventId, OrderStockReleaseEvent event) {
        if (eventId != null) {
            String cacheKey = "stock-release:" + eventId;
            if (idempotencyService.getCachedResponse(cacheKey, event.orderId()) != null) {
                log.warn("Event {} for order {} already processed. Skipping duplicate stock release.",
                        eventId, event.orderId());
                return;
            }
        }

        log.info("Processing stock release for order {} ({} items) [eventId={}]",
                event.orderId(), event.items().size(), eventId);

        int released = 0;
        int failed = 0;

        for (OrderStockReleaseEvent.Item item : event.items()) {
            try {
                productService.increaseQuantity(item.productId(), item.quantity());
                released++;
            } catch (Exception e) {
                failed++;
                log.error("CRITICAL: Failed to release stock for product {} qty {} (order {}). " +
                        "Manual reconciliation required.",
                        item.productId(), item.quantity(), event.orderId(), e);
            }
        }

        log.info("Stock release for order {}: {} released, {} failed [eventId={}]",
                event.orderId(), released, failed, eventId);

        if (eventId != null && failed == 0) {
            idempotencyService.save("stock-release:" + eventId, event.orderId(), Boolean.TRUE);
        }
    }
}
