package com.example.demo.stock.controller;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.service.IdempotencyService;
import com.example.demo.common.service.MessageService;
import com.example.demo.common.utils.ResponseUtils;
import com.example.demo.stock.dto.*;
import com.example.demo.stock.service.StockService;
import com.example.demo.stock.validator.StockValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

import static com.example.demo.common.service.IdempotencyService.IDEMPOTENCY_HEADER;
import static com.example.demo.common.utils.Utils.checkErrors;

@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockValidator stockValidator;

    private final StockService stockService;

    private final IdempotencyService idempotencyService;

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<StockListResponse>>> findAll(@RequestParam(required = false) LocalDateTime fromDate,
                                                                        @RequestParam(required = false) LocalDateTime toDate,
                                                                        Pageable pageable) {

        Page<StockListResponse> stocks = stockService.findAll(fromDate, toDate, pageable);
        return ResponseUtils.ok(stocks, messageService.get("successfully.found", "Stock List"));
    }

    @GetMapping("/items")
    public ResponseEntity<ApiResponse<Page<StockItemResponse>>> findAllItems(@RequestParam(required = false) LocalDateTime fromDate,
                                                                             @RequestParam(required = false) LocalDateTime toDate,
                                                                             @RequestParam(required = false) Long productId,
                                                                             Pageable pageable) {

        Page<StockItemResponse> stocks = stockService.findAllItems(fromDate, toDate, productId, pageable);
        return ResponseUtils.ok(stocks, messageService.get("successfully.found", "Stock Item List"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StockResponse>> findById(@PathVariable Long id) {
        StockResponse stock = stockService.findById(id);
        return ResponseUtils.ok(stock, messageService.get("successfully.found", "Stock"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(@Valid @RequestBody CreateStockRequest stockRequest,
                                                    BindingResult bindingResult,
                                                    @RequestHeader(IDEMPOTENCY_HEADER) String key) {

        Object response = idempotencyService.getCachedResponse(key, stockRequest);
        if (response != null) {
            return ResponseUtils.ok((Long) response, messageService.get("entity.creating", "Stock"));
        }

        stockValidator.validateCreate(stockRequest, bindingResult);
        checkErrors(bindingResult);

        long id = stockService.create(stockRequest);
        idempotencyService.save(key, stockRequest, id);
        return ResponseUtils.created(id, messageService.get("entity.creating", "Stock"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StockResponse>> update(@PathVariable Long id,
                                                             @Valid @RequestBody UpdateStockRequest stockRequest,
                                                             BindingResult bindingResult) {

        stockValidator.validateUpdate(stockRequest, bindingResult);
        checkErrors(bindingResult);

        StockResponse stock = stockService.update(id, stockRequest);
        return ResponseUtils.ok(stock, messageService.get("successfully.updated", "Stock"));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<ApiResponse<StockResponse>> addItem(@PathVariable Long id,
                                                              @Valid @RequestBody CreateStockItemRequest itemRequest,
                                                              BindingResult bindingResult) {

        stockValidator.validateCreateItem(itemRequest, bindingResult);
        checkErrors(bindingResult);

        StockResponse stock = stockService.addItem(id, itemRequest);
        return ResponseUtils.ok(stock, messageService.get("successfully.updated", "Stock"));
    }

    @PutMapping("/{stockId}/items/{itemId}")
    public ResponseEntity<ApiResponse<StockResponse>> updateItem(@PathVariable Long stockId,
                                                                 @PathVariable Long itemId,
                                                                 @Valid @RequestBody UpdateStockItemRequest itemRequest,
                                                                 BindingResult bindingResult) {

        stockValidator.validateUpdateItem(itemRequest, bindingResult);
        checkErrors(bindingResult);

        StockResponse stock = stockService.updateItem(stockId, itemId, itemRequest);
        return ResponseUtils.ok(stock, messageService.get("successfully.updated", "Stock"));
    }

    @DeleteMapping("/{stockId}/items/{itemId}")
    public ResponseEntity<ApiResponse<StockResponse>> removeItem(@PathVariable Long stockId, @PathVariable Long itemId) {
        StockResponse stock = stockService.removeItem(stockId, itemId);
        return ResponseUtils.ok(stock, messageService.get("successfully.updated", "Stock"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        stockService.delete(id);
        return ResponseUtils.ok(messageService.get("successfully.deleted", "Stock"));
    }
}
