package com.example.demo.payment.service;

import com.example.demo.common.config.BkashConfig;
import com.example.demo.payment.dto.CreatePaymentRequest;
import com.example.demo.order.dto.CreateOrderResponse;
import com.example.demo.payment.dto.ExecutePaymentResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
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

    /**
     * Step 1: Create a payment and return the bKash payment URL
     */
    @Override
    public CreateOrderResponse create(CreatePaymentRequest request, Long orderId) {
        HttpHeaders headers = authHeaders();

        Map<String, Object> body = Map.of(
                "mode", "0011",          // Checkout URL mode
                "payerReference", request.userId(),
                "callbackURL", config.getCallbackUrl() + "?orderId=" + orderId,
                "amount", request.amount(),
                "currency", "BDT",
                "intent", "sale",
                "merchantInvoiceNumber", "INV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase()
        );

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

            return CreateOrderResponse.builder()
                    .id(orderId)
                    .paymentID(node.path("paymentID").asText())
                    .paymentURL(node.path("bkashURL").asText())
                    .statusCode(node.path("statusCode").asText())
                    .statusMessage(node.path("statusMessage").asText())
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse create payment response", e);
        }
    }

    /**
     * Step 2: Execute payment after user completes on bKash side
     */
    @Override
    public ExecutePaymentResponse execute(String paymentID) {
        HttpHeaders headers = authHeaders();

        Map<String, String> body = Map.of("paymentID", paymentID);

        String responseBody = bkashWebClient.post()
                .uri( "/execute")
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.debug("bKash execute payment response: {}", responseBody);
        try {
            JsonNode node = objectMapper.readTree(responseBody);

            return ExecutePaymentResponse.builder()
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
    public JsonNode queryPayment(String paymentID) {
        HttpHeaders headers = authHeaders();

        String responseBody = bkashWebClient.get()
                .uri(config.getBaseUrl() + "/payment/status?paymentID=" + paymentID)
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
