package com.example.ecom.payment.service;

import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.payment.dto.CreatePaymentRequest;
import com.example.ecom.payment.dto.CreatePaymentResponse;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

public interface PaymentService {

    String create(CreatePaymentRequest request, CustomUserDetails userDetails);

    CreatePaymentResponse execute(String paymentID);

    JsonNode findByPaymentId(String paymentId);

    JsonNode refundPayment(String paymentID, String trxID, String amount, String reason);

    UUID getOrderIdByPaymentId(String paymentID);

    void updatePaymentStatus(String paymentID, com.example.ecom.payment.dto.CreatePaymentResponse result, boolean success);
}

