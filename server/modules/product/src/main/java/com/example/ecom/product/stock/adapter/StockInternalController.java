package com.example.ecom.product.stock.adapter;

import com.example.ecom.common.client.stock.StockClient;
import com.example.ecom.product.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Internal REST endpoint for cross-service stock operations.
 * Secured by {@code X-Internal-Service-Key} at the Spring Security layer.
 * In monolith mode, these endpoints exist but are unused
 * (callers use {@link StockClientAdapter} directly).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/stock")
public class StockInternalController implements StockClient {

    private final StockService stockService;

    @PostMapping("/{productId}/consume")
    @Override
    public void consume(@PathVariable UUID productId, @RequestParam int quantity, @RequestParam UUID orderId) {
        stockService.consume(productId, quantity, orderId);
    }
}
