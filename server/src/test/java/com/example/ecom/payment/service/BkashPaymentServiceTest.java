package com.example.ecom.payment.service;

import com.example.ecom.payment.config.BkashConfigProperties;
import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.common.client.order.OrderClient;
import com.example.ecom.common.client.order.OrderPaymentDto;
import com.example.ecom.common.enums.Permission;
import com.example.ecom.payment.enums.PaymentStatus;
import com.example.ecom.payment.model.Payment;
import com.example.ecom.payment.dto.CreatePaymentRequest;
import com.example.ecom.payment.dto.CreatePaymentResponse;
import com.example.ecom.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BkashPaymentServiceTest {

    private static final UUID ORDER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OTHER_USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private BkashConfigProperties config;

    @Mock
    private BkashTokenService tokenService;

    @Mock
    private WebClient bkashWebClient;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderClient orderClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private BkashPaymentService bkashPaymentService;

    private OrderPaymentDto testOrderSnapshot;

    @BeforeEach
    void setUp() {
        bkashPaymentService = new BkashPaymentService(
                config, tokenService, bkashWebClient, objectMapper, paymentRepository, orderClient
        );

        testOrderSnapshot = new OrderPaymentDto(
                ORDER_ID, USER_ID, "PENDING", new BigDecimal("1500.00"), false, new ArrayList<>()
        );
    }

    // ========== create ==========

    @Test
    @DisplayName("Should throw EntityNotFoundException when order is not found")
    void create_orderNotFound_throwsEntityNotFoundException() {
        CreatePaymentRequest request = new CreatePaymentRequest(USER_ID, ORDER_ID, new BigDecimal("1500.00"), "ref");

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getId()).thenReturn(USER_ID);
        when(orderClient.findOrderForPayment(eq(ORDER_ID), eq(USER_ID), anyBoolean()))
                .thenThrow(new EntityNotFoundException("Order not found: " + ORDER_ID));

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> bkashPaymentService.create(request, userDetails));

        assertTrue(ex.getMessage().contains(ORDER_ID.toString()));
    }

    @Test
    @DisplayName("Should throw ValidationException when order is already paid")
    void create_orderAlreadyPaid_throwsValidationException() {
        OrderPaymentDto paidOrder = new OrderPaymentDto(
                ORDER_ID, USER_ID, "PENDING", new BigDecimal("1500.00"), true, new ArrayList<>()
        );
        CreatePaymentRequest request = new CreatePaymentRequest(USER_ID, ORDER_ID, new BigDecimal("1500.00"), "ref");

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getId()).thenReturn(USER_ID);
        when(orderClient.findOrderForPayment(eq(ORDER_ID), eq(USER_ID), anyBoolean())).thenReturn(paidOrder);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> bkashPaymentService.create(request, userDetails));

        assertEquals("Order is already paid", ex.getMessage());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ValidationException when order is cancelled")
    void create_orderCancelled_throwsValidationException() {
        OrderPaymentDto cancelledOrder = new OrderPaymentDto(
                ORDER_ID, USER_ID, "CANCELLED", new BigDecimal("1500.00"), false, new ArrayList<>()
        );
        CreatePaymentRequest request = new CreatePaymentRequest(USER_ID, ORDER_ID, new BigDecimal("1500.00"), "ref");

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getId()).thenReturn(USER_ID);
        when(orderClient.findOrderForPayment(eq(ORDER_ID), eq(USER_ID), anyBoolean())).thenReturn(cancelledOrder);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> bkashPaymentService.create(request, userDetails));

        assertEquals("Cannot pay for cancelled or rejected order", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw ValidationException when order is rejected")
    void create_orderRejected_throwsValidationException() {
        OrderPaymentDto rejectedOrder = new OrderPaymentDto(
                ORDER_ID, USER_ID, "REJECTED", new BigDecimal("1500.00"), false, new ArrayList<>()
        );
        CreatePaymentRequest request = new CreatePaymentRequest(USER_ID, ORDER_ID, new BigDecimal("1500.00"), "ref");

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getId()).thenReturn(USER_ID);
        when(orderClient.findOrderForPayment(eq(ORDER_ID), eq(USER_ID), anyBoolean())).thenReturn(rejectedOrder);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> bkashPaymentService.create(request, userDetails));

        assertEquals("Cannot pay for cancelled or rejected order", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw ValidationException when product has insufficient stock")
    void create_insufficientStock_throwsValidationException() {
        CreatePaymentRequest request = new CreatePaymentRequest(USER_ID, ORDER_ID, new BigDecimal("1500.00"), "ref");

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getId()).thenReturn(USER_ID);
        when(orderClient.findOrderForPayment(eq(ORDER_ID), eq(USER_ID), anyBoolean())).thenReturn(testOrderSnapshot);
        doThrow(new ValidationException("Insufficient stock for product: Wireless Mouse"))
                .when(orderClient).validateStockForPayment(ORDER_ID);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> bkashPaymentService.create(request, userDetails));

        assertTrue(ex.getMessage().contains("Insufficient stock"));
        assertTrue(ex.getMessage().contains("Wireless Mouse"));
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when non-owner non-admin tries to pay")
    void create_nonOwnerNonAdmin_throwsAccessDeniedException() {
        CreatePaymentRequest request = new CreatePaymentRequest(OTHER_USER_ID, ORDER_ID, new BigDecimal("1500.00"), "ref");

        CustomUserDetails otherUser = mock(CustomUserDetails.class);
        when(otherUser.getId()).thenReturn(OTHER_USER_ID);

        when(orderClient.findOrderForPayment(eq(ORDER_ID), eq(OTHER_USER_ID), anyBoolean()))
                .thenThrow(new AccessDeniedException("Access denied"));

        assertThrows(AccessDeniedException.class,
                () -> bkashPaymentService.create(request, otherUser));
    }

    @Test
    @DisplayName("Should pass isAdmin=true when admin user pays for another user's order")
    void create_adminUserCanPayForOtherUserOrder() {
        UUID adminId = UUID.randomUUID();
        CreatePaymentRequest request = new CreatePaymentRequest(adminId, ORDER_ID, new BigDecimal("1500.00"), "ref");

        CustomUserDetails adminUser = mock(CustomUserDetails.class);
        when(adminUser.getId()).thenReturn(adminId);
        GrantedAuthority authority = new SimpleGrantedAuthority(Permission.ADMIN_ACCESS.getValue());
        doReturn(List.of(authority)).when(adminUser).getAuthorities();

        when(orderClient.findOrderForPayment(eq(ORDER_ID), eq(adminId), eq(true)))
                .thenReturn(testOrderSnapshot);

        when(tokenService.getValidToken()).thenReturn("Bearer token");
        when(config.getAppKey()).thenReturn("appKey");
        when(config.getBaseUrl()).thenReturn("https://tokenized.sandbox.bka.sh/v2");
        when(config.getCallbackUrl()).thenReturn("https://example.com/callback");

        @SuppressWarnings("rawtypes")
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        @SuppressWarnings("rawtypes")
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        @SuppressWarnings("rawtypes")
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(bkashWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(
                "{\"statusCode\":\"0000\",\"bkashURL\":\"https://payment.bkash.com/pay\",\"paymentID\":\"PAY-123\"}"
        ));

        String paymentUrl = bkashPaymentService.create(request, adminUser);

        assertEquals("https://payment.bkash.com/pay", paymentUrl);
        verify(orderClient).findOrderForPayment(eq(ORDER_ID), eq(adminId), eq(true));
        verify(orderClient).validateStockForPayment(ORDER_ID);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should delegate acceptOrderForPrepayment to orderClient")
    void acceptOrderForPrepayment_delegatesToOrderClient() {
        bkashPaymentService.acceptOrderForPrepayment(ORDER_ID);

        verify(orderClient).acceptOrderForPrepayment(ORDER_ID);
    }

    // ========== getOrderIdByPaymentId ==========

    @Test
    @DisplayName("Should return order ID when payment is found by payment intent ID")
    void getOrderIdByPaymentId_paymentFound_returnsOrderId() {
        Payment payment = new Payment();
        payment.setOrderId(ORDER_ID);
        payment.setPaymentIntentId("PAY-ABC123");

        when(paymentRepository.findByPaymentIntentId("PAY-ABC123")).thenReturn(Optional.of(payment));

        UUID result = bkashPaymentService.getOrderIdByPaymentId("PAY-ABC123");

        assertEquals(ORDER_ID, result);
        verify(paymentRepository).findByPaymentIntentId("PAY-ABC123");
    }

    @Test
    @DisplayName("Should return null when payment is not found by payment intent ID")
    void getOrderIdByPaymentId_paymentNotFound_returnsNull() {
        when(paymentRepository.findByPaymentIntentId("NONEXISTENT")).thenReturn(Optional.empty());

        UUID result = bkashPaymentService.getOrderIdByPaymentId("NONEXISTENT");

        assertNull(result);
        verify(paymentRepository).findByPaymentIntentId("NONEXISTENT");
    }

    // ========== updatePaymentStatus ==========

    @Test
    @DisplayName("Should update payment status to COMPLETED on success")
    void updatePaymentStatus_success_updatesStatusToCompleted() {
        Payment payment = new Payment();
        payment.setPaymentIntentId("PAY-XYZ");
        payment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findByPaymentIntentId("PAY-XYZ")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        CreatePaymentResponse response = CreatePaymentResponse.builder()
                .trxID("TRX-001")
                .merchantInvoiceNumber("INV-MERCHANT001")
                .build();

        bkashPaymentService.updatePaymentStatus("PAY-XYZ", response, true);

        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        assertEquals("TRX-001", payment.getTransactionId());
        assertEquals("INV-MERCHANT001", payment.getMerchantInvoiceNumber());
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("Should update payment status to FAILED on failure")
    void updatePaymentStatus_failure_updatesStatusToFailed() {
        Payment payment = new Payment();
        payment.setPaymentIntentId("PAY-FAIL");
        payment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findByPaymentIntentId("PAY-FAIL")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        bkashPaymentService.updatePaymentStatus("PAY-FAIL", null, false);

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("Should do nothing when payment is not found during status update")
    void updatePaymentStatus_paymentNotFound_doesNothing() {
        when(paymentRepository.findByPaymentIntentId("PAY-MISSING")).thenReturn(Optional.empty());

        bkashPaymentService.updatePaymentStatus("PAY-MISSING", null, false);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should set status to FAILED when success is false even with non-null result")
    void updatePaymentStatus_successFalseWithResult_updatesStatusToFailed() {
        Payment payment = new Payment();
        payment.setPaymentIntentId("PAY-FALSESUCCESS");
        payment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findByPaymentIntentId("PAY-FALSESUCCESS")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        CreatePaymentResponse response = CreatePaymentResponse.builder()
                .trxID("TRX-002")
                .merchantInvoiceNumber("INV-002")
                .build();

        bkashPaymentService.updatePaymentStatus("PAY-FALSESUCCESS", response, false);

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertNull(payment.getTransactionId()); // should not be set because success=false
    }
}
