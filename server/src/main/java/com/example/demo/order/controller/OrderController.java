package com.example.demo.order.controller;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.enums.OrderStatus;
import com.example.demo.common.helper.CommonHelper;
import com.example.demo.common.service.IdempotencyService;
import com.example.demo.common.service.MessageService;
import com.example.demo.common.utils.ResponseUtils;
import com.example.demo.order.dto.CreateOrderRequest;
import com.example.demo.order.dto.OrderListResponse;
import com.example.demo.order.dto.OrderResponse;
import com.example.demo.order.dto.UpdateOrderStatusRequest;
import com.example.demo.order.service.OrderService;
import com.example.demo.order.validator.OrderValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

import static com.example.demo.common.service.IdempotencyService.IDEMPOTENCY_HEADER;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderValidator orderValidator;

    private final CommonHelper commonHelper;

    private final OrderService orderService;

    private final IdempotencyService idempotencyService;

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderListResponse>>> findAll(@RequestParam(required = false) LocalDateTime fromDate,
                                                                        @RequestParam(required = false) LocalDateTime toDate,
                                                                        @RequestParam(required = false) OrderStatus status,
                                                                        Pageable pageable,
                                                                        @AuthenticationPrincipal CustomUserDetails userDetails) {

        Page<OrderListResponse> orders = orderService.findAll(fromDate, toDate, status, userDetails, pageable);
        return ResponseUtils.ok(orders, messageService.get("successfully.found", "Order List"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> findById(@PathVariable Long id,
                                                               @AuthenticationPrincipal CustomUserDetails userDetails) {

        OrderResponse order = orderService.findById(id, userDetails);
        return ResponseUtils.ok(order, messageService.get("successfully.found", "Order"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(@Valid @RequestBody CreateOrderRequest orderRequest,
                                                    BindingResult bindingResult,
                                                    @RequestHeader(IDEMPOTENCY_HEADER) String key,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {

        Object response = idempotencyService.getCachedResponse(key, orderRequest);
        if (response != null) {
            return ResponseUtils.ok((Long) response, messageService.get("entity.creating", "Order"));
        }

        orderValidator.validate(orderRequest, bindingResult);
        commonHelper.checkErrors(bindingResult);

        long id = orderService.create(orderRequest, userDetails);
        idempotencyService.save(key, orderRequest, id);
        return ResponseUtils.created(id, messageService.get("entity.creating", "Order"));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(@PathVariable Long id,
                                                                   @Valid @RequestBody UpdateOrderStatusRequest request,
                                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {

        OrderResponse order = orderService.updateStatus(id, request, userDetails);
        return ResponseUtils.ok(order, messageService.get("successfully.updated", "Order"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseUtils.ok(messageService.get("successfully.deleted", "Order"));
    }
}
