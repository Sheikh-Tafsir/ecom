package com.example.ecom.order.event;

import com.example.ecom.common.event.PaymentCompletedEvent;
import com.example.ecom.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalPaymentCompletedListener {

    private final OrderService orderService;

    @EventListener
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received local Spring event PaymentCompletedEvent for order {}", event.orderId());
        orderService.acceptOrderForPrepayment(event.orderId());
    }
}
