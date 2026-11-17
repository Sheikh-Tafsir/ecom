package com.example.ecom.product.event;

import com.example.ecom.common.event.DomainEvent;
import com.example.ecom.common.event.EventConstants;
import com.example.ecom.common.event.OrderStockReleaseEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RmqOrderStockReleaseListenerTest {

    @Mock
    private OrderStockReleaseService stockReleaseService;

    @InjectMocks
    private RmqOrderStockReleaseListener listener;

    @Test
    @DisplayName("onMessage should delegate eventId and payload to OrderStockReleaseService")
    void onMessage_delegatesToService() {
        OrderStockReleaseEvent payload = new OrderStockReleaseEvent(
                UUID.randomUUID(), List.of(new OrderStockReleaseEvent.Item(UUID.randomUUID(), 1)));
        DomainEvent<OrderStockReleaseEvent> event = DomainEvent.of(
                EventConstants.ORDER_STOCK_RELEASE, EventConstants.SOURCE_ORDER, payload);

        listener.onMessage(event);

        verify(stockReleaseService).releaseStock(event.eventId(), payload);
    }
}
