package com.example.ecom.product.stock.adapter;

import com.example.ecom.common.client.stock.StockClient;
import com.example.ecom.product.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Monolith adapter: implements {@link StockClient} by delegating to {@link StockService}.
 * On microservice extraction, this class is removed and replaced by an HTTP proxy.
 */
@Service
@RequiredArgsConstructor
public class StockClientAdapter implements StockClient {

    private final StockService stockService;

    @Override
    public void consume(UUID productId, int quantity, UUID orderId) {
        stockService.consume(productId, quantity, orderId);
    }
}
