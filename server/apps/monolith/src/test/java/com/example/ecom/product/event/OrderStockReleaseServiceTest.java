package com.example.ecom.product.event;

import com.example.ecom.common.event.OrderStockReleaseEvent;
import com.example.ecom.common.service.IdempotencyService;
import com.example.ecom.product.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderStockReleaseServiceTest {

    @Mock
    private ProductService productService;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private OrderStockReleaseService stockReleaseService;

    @Test
    @DisplayName("releaseStock should release stock for each item in the order")
    void releaseStock_releasesStockForAllItems() {
        UUID prod1 = UUID.randomUUID();
        UUID prod2 = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        List<OrderStockReleaseEvent.Item> items = List.of(
                new OrderStockReleaseEvent.Item(prod1, 2),
                new OrderStockReleaseEvent.Item(prod2, 5)
        );
        OrderStockReleaseEvent event = new OrderStockReleaseEvent(orderId, items);

        stockReleaseService.releaseStock(event);

        verify(productService).increaseQuantity(prod1, 2);
        verify(productService).increaseQuantity(prod2, 5);
    }

    @Test
    @DisplayName("releaseStock should not throw when a single item release fails (partial failure)")
    void releaseStock_continuesOnSingleItemFailure() {
        UUID prod1 = UUID.randomUUID();
        UUID prod2 = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        List<OrderStockReleaseEvent.Item> items = List.of(
                new OrderStockReleaseEvent.Item(prod1, 2),
                new OrderStockReleaseEvent.Item(prod2, 5)
        );
        OrderStockReleaseEvent event = new OrderStockReleaseEvent(orderId, items);

        doThrow(new RuntimeException("Product not found")).when(productService).increaseQuantity(prod1, 2);

        stockReleaseService.releaseStock(event);

        verify(productService).increaseQuantity(prod1, 2);
        verify(productService).increaseQuantity(prod2, 5); // prod2 still processed
    }

    @Test
    @DisplayName("releaseStock with eventId should skip processing if event was already processed")
    void releaseStock_skipsDuplicateEvent() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        OrderStockReleaseEvent event = new OrderStockReleaseEvent(
                orderId, List.of(new OrderStockReleaseEvent.Item(UUID.randomUUID(), 2)));

        when(idempotencyService.getCachedResponse("stock-release:" + eventId, orderId)).thenReturn(Boolean.TRUE);

        stockReleaseService.releaseStock(eventId, event);

        verifyNoInteractions(productService);
        verify(idempotencyService, never()).save(anyString(), any(), any());
    }

    @Test
    @DisplayName("releaseStock with eventId should mark event as processed after successful run")
    void releaseStock_marksEventAsProcessed() {
        UUID eventId = UUID.randomUUID();
        UUID prodId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        OrderStockReleaseEvent event = new OrderStockReleaseEvent(
                orderId, List.of(new OrderStockReleaseEvent.Item(prodId, 2)));

        when(idempotencyService.getCachedResponse("stock-release:" + eventId, orderId)).thenReturn(null);

        stockReleaseService.releaseStock(eventId, event);

        verify(productService).increaseQuantity(prodId, 2);
        verify(idempotencyService).save("stock-release:" + eventId, orderId, Boolean.TRUE);
    }

    @Test
    @DisplayName("releaseStock with eventId should not mark event as processed if an item fails")
    void releaseStock_doesNotMarkIdempotentOnFailure() {
        UUID eventId = UUID.randomUUID();
        UUID prodId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        OrderStockReleaseEvent event = new OrderStockReleaseEvent(
                orderId, List.of(new OrderStockReleaseEvent.Item(prodId, 2)));

        when(idempotencyService.getCachedResponse("stock-release:" + eventId, orderId)).thenReturn(null);
        doThrow(new RuntimeException("DB error")).when(productService).increaseQuantity(prodId, 2);

        stockReleaseService.releaseStock(eventId, event);

        verify(idempotencyService, never()).save(anyString(), any(), any());
    }
}
