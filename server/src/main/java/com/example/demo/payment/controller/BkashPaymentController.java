package com.example.demo.payment.controller;

import com.example.demo.common.config.BkashConfig;
import com.example.demo.order.service.OrderService;
import com.example.demo.payment.dto.ExecutePaymentResponse;
import com.example.demo.payment.service.BkashPaymentService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class BkashPaymentController {

    private final BkashPaymentService paymentService;

    private final OrderService orderService;

    private final BkashConfig config;

//    /**
//     * Called by React to initiate payment
//     */
//    @PostMapping("/create")
//    public ResponseEntity<ApiResponse<CreateOrderResponse>> createPayment(@RequestBody CreatePaymentRequest request) {
//        CreateOrderResponse response = paymentService.create(request);
//        return ResponseUtils.ok(response, "Payment initiated");
//    }

    /**
     * bKash redirects here after user completes/cancels/fails payment.
     * This is a GET callback — we execute the payment, then redirect to React.
     */
    @GetMapping("/callback")
    public void callback(@RequestParam String paymentID,
                         @RequestParam String status,
                         @RequestParam long orderId,
                         HttpServletResponse httpResponse) throws IOException {

        log.info("Callback hit — paymentID={}, status={}, orderId={}", paymentID, status, orderId);

        if (!"success".equalsIgnoreCase(status)) {
            log.warn("payment failed/cancelled. paymentID={}, status={}", paymentID, status);
            orderService.delete(orderId);

            httpResponse.sendRedirect(config.getFrontendFailUrl()
                    + "?orderId=" + orderId
                    + "&status=" + status);
            return;
        }

        try {
            ExecutePaymentResponse result = paymentService.execute(paymentID);

            if ("0000".equals(result.getStatusCode())) {
                orderService.acceptOrder(orderId);

                httpResponse.sendRedirect(config.getFrontendSuccessUrl()
                        + "?orderId=" + orderId
                        + "&trxID=" + result.getTrxID()
                        + "&status=success");
            } else {
                orderService.delete(orderId);

                httpResponse.sendRedirect(config.getFrontendFailUrl()
                        + "?orderId=" + orderId
                        + "&status=failure");
            }
        } catch (Exception e) {
            log.error("Execute payment error", e);
            httpResponse.sendRedirect(config.getFrontendFailUrl() +
                    "?paymentID=" + paymentID
                    + "&reason=server_error");
        }
    }

    /**
     * Query payment status
     */
    @GetMapping("/query/{paymentID}")
    public ResponseEntity<?> queryPayment(@PathVariable String paymentID) {
        return ResponseEntity.ok(paymentService.queryPayment(paymentID));
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
