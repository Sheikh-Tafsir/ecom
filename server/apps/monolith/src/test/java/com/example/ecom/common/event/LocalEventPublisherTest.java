package com.example.ecom.common.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocalEventPublisherTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private LocalEventPublisher publisher;

    @Test
    @DisplayName("publish dispatches any payload locally via Spring ApplicationEventPublisher")
    void publish_dispatchesPayloadLocally() {
        OrderStockReleaseEvent payload = new OrderStockReleaseEvent(
                UUID.randomUUID(), List.of(new OrderStockReleaseEvent.Item(UUID.randomUUID(), 2)));

        publisher.publish(EventConstants.SOURCE_ORDER, EventConstants.ORDER_STOCK_RELEASE, payload);

        verify(applicationEventPublisher).publishEvent(payload);
    }
}
