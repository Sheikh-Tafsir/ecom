package com.example.demo.order.controller;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.enums.OrderStatus;
import com.example.demo.common.service.MessageService;
import com.example.demo.common.utils.ResponseUtils;
import com.example.demo.order.dto.*;
import com.example.demo.order.service.OrderService;
import com.example.demo.order.validator.OrderValidator;
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

import static com.example.demo.common.service.IdempotencyService.IDEMPOTENCY_HEADER;
import static com.example.demo.common.utils.Utils.checkErrors;

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

    @PostMapping
    public ResponseEntity<ApiResponse<CreateOrderResponse>> create(@Valid @RequestBody CreateOrderRequest request,
                                                                   BindingResult bindingResult,
                                                                   @RequestHeader(IDEMPOTENCY_HEADER) String idempotencyKey,
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
