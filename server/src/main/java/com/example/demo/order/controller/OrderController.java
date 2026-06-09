package com.example.demo.order.controller;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.enums.OrderStatus;
import com.example.demo.common.helper.CommonHelper;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderValidator orderValidator;

    private final CommonHelper commonHelper;

    private final OrderService orderService;

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderListResponse>>> findAll(Pageable pageable,
                                                                        @RequestParam(required = false) OrderStatus status) {

        Page<OrderListResponse> orders = orderService.findAll(pageable, status);
        return ResponseUtils.ok(orders, messageService.get("successfully.found", "Order List"));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<OrderListResponse>>> findMyOrders(Pageable pageable,
                                                                             @AuthenticationPrincipal CustomUserDetails userDetails) {

        Page<OrderListResponse> orders = orderService.findByUser(userDetails.user().getId(), pageable);
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
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {

        orderValidator.validate(orderRequest, bindingResult);
        commonHelper.checkErrors(bindingResult);

        long id = orderService.create(orderRequest, userDetails);
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
