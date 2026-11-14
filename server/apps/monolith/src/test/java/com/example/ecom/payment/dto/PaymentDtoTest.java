package com.example.ecom.payment.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentDtoTest {

    @Nested
    @DisplayName("CreatePaymentRequest Tests")
    class CreatePaymentRequestTest {

        @Test
        @DisplayName("Record constructor sets all fields")
        void constructor_validArgs_setsFields() {
            UUID userId = UUID.fromString("01927d75-097f-7afa-9ae7-960718293041");
            UUID orderId = UUID.fromString("01927d75-1a8f-7bfa-8ae8-071829304152");

            CreatePaymentRequest request = new CreatePaymentRequest(
                    userId, orderId, new BigDecimal("1500.00"), "01712345678"
            );

            assertEquals(userId, request.userId());
            assertEquals(orderId, request.orderId());
            assertEquals(new BigDecimal("1500.00"), request.amount());
            assertEquals("01712345678", request.payerReference());
        }

        @Test
        @DisplayName("Two equal records are equal")
        void equals_sameFields_returnsTrue() {
            UUID uid = UUID.fromString("01927d75-2b9f-7cfa-7ae9-182930415263");
            UUID oid = UUID.fromString("01927d75-3caf-7dfa-6aea-293041526374");

            CreatePaymentRequest r1 = new CreatePaymentRequest(uid, oid, new BigDecimal("100.00"), "ref");
            CreatePaymentRequest r2 = new CreatePaymentRequest(uid, oid, new BigDecimal("100.00"), "ref");

            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
        }
    }

    @Nested
    @DisplayName("CreatePaymentResponse Tests")
    class CreatePaymentResponseTest {

        @Test
        @DisplayName("Builder creates response with all fields")
        void builder_validArgs_setsFields() {
            CreatePaymentResponse response = CreatePaymentResponse.builder()
                    .paymentID("PAY-12345")
                    .trxID("TRX-67890")
                    .transactionStatus("Completed")
                    .amount("1500.00")
                    .currency("BDT")
                    .merchantInvoiceNumber("INV-001")
                    .statusCode("0000")
                    .statusMessage("Successful")
                    .build();

            assertEquals("PAY-12345", response.getPaymentID());
            assertEquals("TRX-67890", response.getTrxID());
            assertEquals("Completed", response.getTransactionStatus());
            assertEquals("1500.00", response.getAmount());
            assertEquals("BDT", response.getCurrency());
            assertEquals("INV-001", response.getMerchantInvoiceNumber());
            assertEquals("0000", response.getStatusCode());
            assertEquals("Successful", response.getStatusMessage());
        }

        @Test
        @DisplayName("Setters update all fields")
        void setters_allFields_updateCorrectly() {
            CreatePaymentResponse response = CreatePaymentResponse.builder().build();
            response.setPaymentID("PAY-UPDATED");
            response.setTrxID("TRX-UPDATED");
            response.setTransactionStatus("Failed");
            response.setAmount("500.00");
            response.setCurrency("USD");
            response.setMerchantInvoiceNumber("INV-002");
            response.setStatusCode("1001");
            response.setStatusMessage("Payment failed");

            assertEquals("PAY-UPDATED", response.getPaymentID());
            assertEquals("TRX-UPDATED", response.getTrxID());
            assertEquals("Failed", response.getTransactionStatus());
            assertEquals("500.00", response.getAmount());
            assertEquals("USD", response.getCurrency());
            assertEquals("INV-002", response.getMerchantInvoiceNumber());
            assertEquals("1001", response.getStatusCode());
            assertEquals("Payment failed", response.getStatusMessage());
        }
    }
}
