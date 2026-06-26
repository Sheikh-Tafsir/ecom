package com.example.demo.payment.service;

import com.example.demo.payment.dto.CreatePaymentRequest;
import com.example.demo.payment.dto.CreatePaymentResponse;
import com.fasterxml.jackson.databind.JsonNode;

public interface PaymentService {

    String create(CreatePaymentRequest request);

    CreatePaymentResponse execute(String paymentID);

    JsonNode findByPaymentId(String paymentId);

    JsonNode refundPayment(String paymentID, String trxID, String amount, String reason);

    Long getOrderIdByPaymentId(String paymentID);

    void updatePaymentStatus(String paymentID, com.example.demo.payment.dto.CreatePaymentResponse result, boolean success);
}
