package com.example.demo.payment.service;

import com.example.demo.common.config.BkashConfig;
import com.example.demo.common.enums.PaymentStatus;
import com.example.demo.common.model.Payment;
import com.example.demo.payment.dto.CreatePaymentRequest;
import com.example.demo.payment.dto.CreatePaymentResponse;
import com.example.demo.payment.repository.PaymentRepository;
import com.example.demo.common.model.Order;
import com.example.demo.common.model.OrderItem;
import com.example.demo.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BkashPaymentService implements PaymentService {

    private final BkashConfig config;
    private final BkashTokenService tokenService;
    private final WebClient bkashWebClient;
    private final ObjectMapper objectMapper;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    /**
     * Step 1: Create a payment and return the bKash payment URL
     */
    @Override
    @Transactional
    public String create(CreatePaymentRequest request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + request.orderId()));
        
        for (OrderItem item : order.getItems()) {
            if (item.getProduct().getQuantity() < item.getQuantity()) {
                throw new ValidationException("Insufficient stock for product: " + item.getProduct().getName());
            }
        }

        HttpHeaders headers = authHeaders();

        String merchantInvoiceNumber = "INV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 11).toUpperCase();

        Map<String, Object> body = Map.of(
                "mode", "0011",          // Checkout URL mode
                "payerReference", "Order_" + request.orderId(),
                "callbackURL", config.getCallbackUrl(),
                "amount", request.amount().setScale(2, java.math.RoundingMode.HALF_UP).toString(),
                "currency", "BDT",
                "intent", "sale",
                "merchantInvoiceNumber", merchantInvoiceNumber
        );

        log.info("Requesting bKash payment with body: {}", body);

        String responseBody = bkashWebClient.post()
                .uri(config.getBaseUrl() + "/create")
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.debug("bKash create payment response: {}", responseBody);
        try {
            JsonNode node = objectMapper.readTree(responseBody);

            if (!"0000".equals(node.path("statusCode").asText())) {
                throw new RuntimeException("bKash create payment failed: " + node.path("statusMessage").asText());
            }

            String bkashURL = node.path("bkashURL").asText();
            String paymentID = node.path("paymentID").asText();

            Payment payment = new Payment();
            payment.setOrderId(request.orderId());
            payment.setPaymentIntentId(paymentID);
            payment.setMerchantInvoiceNumber(merchantInvoiceNumber);
            payment.setAmount(request.amount());
            payment.setStatus(PaymentStatus.PENDING);
            
            paymentRepository.save(payment);

            return bkashURL;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse create payment response", e);
        }
    }

    @Override
    public Long getOrderIdByPaymentId(String paymentID) {
        return paymentRepository.findByPaymentIntentId(paymentID)
                .map(Payment::getOrderId)
                .orElse(null);
    }

    @Override
    @Transactional
    public void updatePaymentStatus(String paymentID, CreatePaymentResponse result, boolean success) {
        paymentRepository.findByPaymentIntentId(paymentID).ifPresent(payment -> {
            if (success && result != null) {
                payment.setTransactionId(result.getTrxID());
                payment.setMerchantInvoiceNumber(result.getMerchantInvoiceNumber());
                payment.setStatus(PaymentStatus.COMPLETED);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
            }
            paymentRepository.save(payment);
        });
    }

    /**
     * Step 2: Execute payment after user completes on bKash side
     */
    @Override
    public CreatePaymentResponse execute(String paymentID) {
        HttpHeaders headers = authHeaders();

        Map<String, String> body = Map.of("paymentID", paymentID);

        String responseBody = bkashWebClient.post()
                .uri(config.getBaseUrl() + "/execute")
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.debug("bKash execute payment response: {}", responseBody);
        try {
            JsonNode node = objectMapper.readTree(responseBody);

            return CreatePaymentResponse.builder()
                    .paymentID(node.path("paymentID").asText())
                    .trxID(node.path("trxID").asText())
                    .transactionStatus(node.path("transactionStatus").asText())
                    .amount(node.path("amount").asText())
                    .currency(node.path("currency").asText())
                    .merchantInvoiceNumber(node.path("merchantInvoiceNumber").asText())
                    .statusCode(node.path("statusCode").asText())
                    .statusMessage(node.path("statusMessage").asText())
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse execute payment response", e);
        }
    }

    /**
     * Query payment status
     */
    @Override
    public JsonNode findByPaymentId(String paymentId) {
        HttpHeaders headers = authHeaders();

        String responseBody = bkashWebClient.get()
                .uri(config.getBaseUrl() + "/payment/status?paymentID=" + paymentId)
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception e) {
            throw new RuntimeException("Failed to query payment", e);
        }
    }

    /**
     * Refund a payment
     */
    @Override
    public JsonNode refundPayment(String paymentID, String trxID, String amount, String reason) {
        HttpHeaders headers = authHeaders();

        Map<String, String> body = Map.of(
                "paymentID", paymentID,
                "trxID", trxID,
                "amount", amount,
                "reason", reason,
                "sku", "refund"
        );

        String responseBody = bkashWebClient.post()
                .uri(config.getBaseUrl() + "/payment/refund")
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception e) {
            throw new RuntimeException("Failed to refund payment", e);
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");
        headers.set("Authorization", tokenService.getValidToken());
        headers.set("X-APP-Key", config.getAppKey());
        return headers;
    }
}
