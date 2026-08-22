package com.example.ecom.order.controller;

import com.example.ecom.common.annotation.Idempotent;
import com.example.ecom.common.dto.ApiResponse;
import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.common.enums.OrderStatus;
import com.example.ecom.common.service.MessageService;
import com.example.ecom.common.utils.ResponseUtils;
import com.example.ecom.order.dto.*;
import com.example.ecom.order.service.OrderService;
import com.example.ecom.order.validator.OrderValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PastOrPresent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static com.example.ecom.common.service.IdempotencyService.IDEMPOTENCY_HEADER;
import static com.example.ecom.common.utils.Utils.checkErrors;

@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderValidator orderValidator;

    private final OrderService orderService;

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderListResponse>>> findAll(@RequestParam(required = false) @PastOrPresent LocalDate fromDate,
                                                                        @RequestParam(required = false) @PastOrPresent LocalDate toDate,
                                                                        @RequestParam(required = false) OrderStatus status,
                                                                        @RequestParam(required = false) String productName,
                                                                        Pageable pageable,
                                                                        @AuthenticationPrincipal CustomUserDetails userDetails) {

        Page<OrderListResponse> orders = orderService.findAll(fromDate, toDate, status, productName, userDetails, pageable);
        return ResponseUtils.ok(orders, messageService.get("successfully.found", "Order List"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> findById(@PathVariable Long id) {

        OrderResponse order = orderService.findById(id);
        return ResponseUtils.ok(order, messageService.get("successfully.found", "Order"));
    }

    @Idempotent
    @PostMapping
    public ResponseEntity<ApiResponse<CreateOrderResponse>> create(@Valid @RequestBody CreateOrderRequest request,
                                                                   BindingResult bindingResult,
                                                                   @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
                                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("request {}", request);
        orderValidator.validate(request, bindingResult);
        checkErrors(bindingResult);

        CreateOrderResponse response = orderService.create(request, idempotencyKey, userDetails);
        return ResponseUtils.created(response, messageService.get("entity.creating", "Order"));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(@PathVariable Long id,
                                                             @AuthenticationPrincipal CustomUserDetails userDetails) {

        OrderResponse order = orderService.cancel(id, userDetails);
        return ResponseUtils.ok(order, messageService.get("successfully.updated", "Order"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(@PathVariable Long id,
                                                                   @Valid @RequestBody UpdateOrderStatusRequest request,
                                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {

        OrderResponse order = orderService.updateStatus(id, request, userDetails);
        return ResponseUtils.ok(order, messageService.get("successfully.updated", "Order"));
    }
}
