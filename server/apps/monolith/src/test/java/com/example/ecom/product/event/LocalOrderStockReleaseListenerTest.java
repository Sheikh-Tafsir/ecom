package com.example.ecom.product.event;

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
class LocalOrderStockReleaseListenerTest {

    @Mock
    private OrderStockReleaseService stockReleaseService;

    @InjectMocks
    private LocalOrderStockReleaseListener listener;

    @Test
    @DisplayName("onOrderStockRelease should delegate directly to OrderStockReleaseService")
    void onOrderStockRelease_delegatesToService() {
        UUID orderId = UUID.randomUUID();
        OrderStockReleaseEvent event = new OrderStockReleaseEvent(
                orderId, List.of(new OrderStockReleaseEvent.Item(UUID.randomUUID(), 3)));

        listener.onOrderStockRelease(event);

        verify(stockReleaseService).releaseStock(event);
    }
}
