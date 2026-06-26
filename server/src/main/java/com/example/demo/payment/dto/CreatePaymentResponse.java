package com.example.demo.payment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreatePaymentResponse {
    private String paymentID;
    private String trxID;
    private String transactionStatus;
    private String amount;
    private String currency;
    private String merchantInvoiceNumber;
    private String statusCode;
    private String statusMessage;
}
