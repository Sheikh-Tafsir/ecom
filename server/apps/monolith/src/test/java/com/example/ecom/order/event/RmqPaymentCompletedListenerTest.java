package com.example.ecom.order.event;

import com.example.ecom.common.event.DomainEvent;
import com.example.ecom.common.event.EventConstants;
import com.example.ecom.common.event.PaymentCompletedEvent;
import com.example.ecom.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RmqPaymentCompletedListenerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private RmqPaymentCompletedListener listener;

    @Test
    @DisplayName("onMessage should delegate orderId directly to OrderService.acceptOrderForPrepayment")
    void onMessage_delegatesToOrderService() {
        UUID orderId = UUID.randomUUID();
        PaymentCompletedEvent payload = new PaymentCompletedEvent(orderId, "pay-1", "trx-1");
        DomainEvent<PaymentCompletedEvent> event = DomainEvent.of(
                EventConstants.PAYMENT_COMPLETED, EventConstants.SOURCE_PAYMENT, payload);

        listener.onMessage(event);

        verify(orderService).acceptOrderForPrepayment(orderId);
    }

    @Test
    @DisplayName("onMessage should rethrow exceptions so Spring AMQP routes to DLQ")
    void onMessage_rethrowsExceptionForDlq() {
        UUID orderId = UUID.randomUUID();
        PaymentCompletedEvent payload = new PaymentCompletedEvent(orderId, "pay-1", "trx-1");
        DomainEvent<PaymentCompletedEvent> event = DomainEvent.of(
                EventConstants.PAYMENT_COMPLETED, EventConstants.SOURCE_PAYMENT, payload);

        doThrow(new RuntimeException("DB down")).when(orderService).acceptOrderForPrepayment(orderId);

        assertThatThrownBy(() -> listener.onMessage(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB down");
    }
}
