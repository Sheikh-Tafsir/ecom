package com.example.ecom.product.stock.adapter;

import com.example.ecom.product.stock.service.StockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockClientAdapterTest {

    @Mock
    private StockService stockService;

    @InjectMocks
    private StockClientAdapter stockClientAdapter;

    @Test
    @DisplayName("consume should delegate to stockService.consume")
    void consume_delegatesToStockService() {
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        int quantity = 5;

        stockClientAdapter.consume(productId, quantity, orderId);

        verify(stockService).consume(productId, quantity, orderId);
    }
}
