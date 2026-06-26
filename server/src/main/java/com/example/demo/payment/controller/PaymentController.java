package com.example.demo.payment.controller;

import com.example.demo.common.config.BkashConfig;
import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.utils.ResponseUtils;
import com.example.demo.order.service.OrderService;
import com.example.demo.payment.dto.CreatePaymentRequest;
import com.example.demo.payment.dto.CreatePaymentResponse;
import com.example.demo.payment.service.BkashPaymentService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final BkashPaymentService paymentService;

    private final OrderService orderService;

    private final BkashConfig config;

    /**
     * Called by React to initiate payment
     */
    @PostMapping
    public ResponseEntity<ApiResponse<String>> createPayment(@RequestBody CreatePaymentRequest request) {
        String bkashURL = paymentService.create(request);;
        return ResponseUtils.ok(bkashURL, "found");
    }

    /**
     * bKash redirects here after user completes/cancels/fails payment.
     * This is a GET callback — we execute the payment, then redirect to React.
     */
    @GetMapping("/callback")
    public void callback(@RequestParam String paymentID,
                         @RequestParam String status,
                         HttpServletResponse httpResponse) throws IOException {

        log.info("Callback hit — paymentID={}, status={}", paymentID, status);

        Long orderId = paymentService.getOrderIdByPaymentId(paymentID);

        if (orderId == null) {
            log.error("Order not found for paymentID: {}", paymentID);
            httpResponse.sendRedirect(config.getFrontendFailUrl() + "?reason=not_found");
            return;
        }

        if (!"success".equalsIgnoreCase(status)) {
            log.warn("payment failed/cancelled. paymentID={}, status={}", paymentID, status);
            paymentService.updatePaymentStatus(paymentID, null, false);

            httpResponse.sendRedirect(config.getFrontendFailUrl()
                    + "?orderId=" + orderId
                    + "&status=" + status);
            return;
        }

        try {
            CreatePaymentResponse result = paymentService.execute(paymentID);

            if ("0000".equals(result.getStatusCode())) {
                orderService.acceptOrder(orderId);
                paymentService.updatePaymentStatus(paymentID, result, true);

                httpResponse.sendRedirect(config.getFrontendSuccessUrl()
                        + "?orderId=" + orderId
                        + "&paymentID=" + result.getPaymentID()
                        + "&trxID=" + result.getTrxID()
                        + "&amount=" + result.getAmount()
                        + "&status=success");
            } else {
                paymentService.updatePaymentStatus(paymentID, result, false);

                httpResponse.sendRedirect(config.getFrontendFailUrl()
                        + "?orderId=" + orderId
                        + "&status=failure"
                        + "&reason=" + result.getStatusMessage());
            }
        } catch (Exception e) {
            log.error("Execute payment error", e);
            paymentService.updatePaymentStatus(paymentID, null, false);

            httpResponse.sendRedirect(config.getFrontendFailUrl()
                    + "?orderId=" + orderId
                    + "&reason=server_error");
        }
    }

    /**
     * Query payment status
     */
    @GetMapping("/query/{paymentId}")
    public ResponseEntity<?> findById(@PathVariable String paymentId) {
        return ResponseEntity.ok(paymentService.findByPaymentId(paymentId));
    }

    /**
     * Refund endpoint
     */
    @PostMapping("/refund")
    public ResponseEntity<?> refundPayment(@RequestParam String paymentID,
                                           @RequestParam String trxID,
                                           @RequestParam String amount,
                                           @RequestParam(defaultValue = "Customer request") String reason) {

        return ResponseEntity.ok(paymentService.refundPayment(paymentID, trxID, amount, reason));
    }
}
