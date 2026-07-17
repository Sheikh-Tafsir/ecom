package com.example.demo.order.scheduler;

import com.example.demo.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderRejectScheduler {

    private final OrderService orderService;

    /**
     * Runs every day at midnight to notify admins about old pending orders
     * and reject orders pending for more than 3 days.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupPendingOrders() {
        log.info("Starting scheduled pending order cleanup task");
        orderService.notifyAndRejectPendingOrders();
        log.info("Finished scheduled pending order cleanup task");
    }
}
