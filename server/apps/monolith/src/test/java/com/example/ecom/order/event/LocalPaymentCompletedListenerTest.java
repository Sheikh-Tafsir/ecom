package com.example.ecom.order.event;

import com.example.ecom.common.event.PaymentCompletedEvent;
import com.example.ecom.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocalPaymentCompletedListenerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private LocalPaymentCompletedListener listener;

    @Test
    @DisplayName("onPaymentCompleted should call OrderService.acceptOrderForPrepayment")
    void onPaymentCompleted_callsOrderService() {
        UUID orderId = UUID.randomUUID();
        PaymentCompletedEvent event = new PaymentCompletedEvent(orderId, "pay-123", "trx-456");

        listener.onPaymentCompleted(event);

        verify(orderService).acceptOrderForPrepayment(orderId);
    }
}
