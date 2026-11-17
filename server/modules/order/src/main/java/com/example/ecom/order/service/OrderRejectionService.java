package com.example.ecom.order.service;

import com.example.ecom.order.enums.OrderStatus;
import com.example.ecom.common.event.EventConstants;
import com.example.ecom.common.event.EventPublisher;
import com.example.ecom.common.event.OrderStockReleaseEvent;
import com.example.ecom.order.model.Order;
import com.example.ecom.order.model.OrderStatusHistory;
import com.example.ecom.order.repository.OrderRepository;
import com.example.ecom.order.repository.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Dedicated service for auto-rejecting inactive orders.
 * <p>
 * Extracted from {@link OrderService} to prevent self-invocation of {@code @Transactional}
 * methods, ensuring each expired order is rejected in its own independent transaction
 * ({@link Propagation#REQUIRES_NEW}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderRejectionService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final EventPublisher eventPublisher;

    /**
     * Rejects a single order within its own new transaction so failure in one
     * order does not rollback the entire batch, and the stock-release event fires
     * immediately after this transaction commits.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rejectSingleOrder(Order order) {
        log.info("Rejecting order {} due to inactivity (more than 2 days old)", order.getId());

        OrderStatusHistory history = new OrderStatusHistory(
                order, OrderStatus.PENDING, OrderStatus.REJECTED, null, "Auto-rejected due to inactivity (>2 days)");
        orderStatusHistoryRepository.save(history);

        if (order.getStatusHistories() != null) {
            order.getStatusHistories().add(history);
        }

        order.setStatus(OrderStatus.REJECTED);
        orderRepository.save(order);

        publishStockReleaseEvent(order);
    }

    private void publishStockReleaseEvent(Order order) {
        List<OrderStockReleaseEvent.Item> items = order.getItems().stream()
                .map(item -> new OrderStockReleaseEvent.Item(item.getProductId(), item.getQuantity()))
                .toList();

        eventPublisher.publish(
                EventConstants.SOURCE_ORDER,
                EventConstants.ORDER_STOCK_RELEASE,
                new OrderStockReleaseEvent(order.getId(), items)
        );
    }
}
