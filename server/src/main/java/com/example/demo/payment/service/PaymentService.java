package com.example.demo.payment.service;

import com.example.demo.payment.dto.CreatePaymentRequest;
import com.example.demo.order.dto.CreateOrderResponse;
import com.example.demo.payment.dto.ExecutePaymentResponse;
import com.fasterxml.jackson.databind.JsonNode;

public interface PaymentService {

    CreateOrderResponse create(CreatePaymentRequest request, Long orderId);

    ExecutePaymentResponse execute(String paymentID);

    JsonNode queryPayment(String paymentID);

    JsonNode refundPayment(String paymentID, String trxID, String amount, String reason);
}
