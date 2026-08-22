package com.example.ecom.product.stock.controller;

import com.example.ecom.common.annotation.Idempotent;
import com.example.ecom.common.dto.ApiResponse;
import com.example.ecom.common.service.MessageService;
import com.example.ecom.common.utils.ResponseUtils;
import com.example.ecom.product.stock.dto.*;
import com.example.ecom.product.stock.service.StockService;
import com.example.ecom.product.stock.validator.StockValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PastOrPresent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static com.example.ecom.common.service.IdempotencyService.IDEMPOTENCY_HEADER;
import static com.example.ecom.common.utils.Utils.checkErrors;

@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockValidator stockValidator;

    private final StockService stockService;

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<StockListResponse>>> findAll(@RequestParam(required = false) @PastOrPresent LocalDate fromDate,
                                                                        @RequestParam(required = false) @PastOrPresent LocalDate toDate,
                                                                        @RequestParam(required = false) String productName,
                                                                        Pageable pageable) {

        Page<StockListResponse> stocks = stockService.findAll(fromDate, toDate, productName, pageable);
        return ResponseUtils.ok(stocks, messageService.get("successfully.found", "Stock List"));
    }

    @GetMapping("/items")
    public ResponseEntity<ApiResponse<Page<StockItemResponse>>> findAllItems(@RequestParam(required = false) @PastOrPresent LocalDate fromDate,
                                                                             @RequestParam(required = false) @PastOrPresent LocalDate toDate,
                                                                             @RequestParam(required = false) Long productId,
                                                                             @RequestParam(required = false) String productName,
                                                                             Pageable pageable) {

        Page<StockItemResponse> stocks = stockService.findAllItems(fromDate, toDate, productId, productName, pageable);
        return ResponseUtils.ok(stocks, messageService.get("successfully.found", "Stock Item List"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StockResponse>> findById(@PathVariable Long id) {
        StockResponse stock = stockService.findById(id);
        return ResponseUtils.ok(stock, messageService.get("successfully.found", "Stock"));
    }

    @Idempotent
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(@Valid @RequestBody CreateStockRequest stockRequest,
                                                    BindingResult bindingResult,
                                                    @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String key) {

        stockValidator.validateCreate(stockRequest, bindingResult);
        checkErrors(bindingResult);

        long id = stockService.create(stockRequest, key);
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
