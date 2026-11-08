package com.example.ecom.payment.service;

import com.example.ecom.payment.config.BkashConfigProperties;
import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.order.enums.OrderStatus;
import com.example.ecom.payment.enums.PaymentStatus;
import com.example.ecom.order.model.Order;
import com.example.ecom.payment.model.Payment;
import com.example.ecom.order.service.OrderService;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    private OrderService orderService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private BkashPaymentService bkashPaymentService;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        bkashPaymentService = new BkashPaymentService(
                config, tokenService, bkashWebClient, objectMapper, paymentRepository, orderService
        );

        testOrder = new Order();
        testOrder.setId(ORDER_ID);
        testOrder.setUserId(USER_ID);
        testOrder.setTotalPrice(new BigDecimal("1500.00"));
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setPaid(false);
        testOrder.setItems(new HashSet<>());
    }

    // ========== create ==========

    @Test
    @DisplayName("Should throw EntityNotFoundException when order is not found")
    void create_orderNotFound_throwsEntityNotFoundException() {
        CreatePaymentRequest request = new CreatePaymentRequest(USER_ID, ORDER_ID, new BigDecimal("1500.00"), "ref");

        when(orderService.findByIdHelper(eq(ORDER_ID), any())).thenThrow(new EntityNotFoundException("Order not found: " + ORDER_ID));

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> bkashPaymentService.create(request, null));

        assertTrue(ex.getMessage().contains(ORDER_ID.toString()));
        verify(orderService).findByIdHelper(eq(ORDER_ID), any());
    }

    @Test
    @DisplayName("Should throw ValidationException when order is already paid")
    void create_orderAlreadyPaid_throwsValidationException() {
        testOrder.setPaid(true);
        CreatePaymentRequest request = new CreatePaymentRequest(USER_ID, ORDER_ID, new BigDecimal("1500.00"), "ref");

        when(orderService.findByIdHelper(eq(ORDER_ID), any())).thenReturn(testOrder);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> bkashPaymentService.create(request, null));

        assertEquals("Order is already paid", ex.getMessage());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ValidationException when order is cancelled")
    void create_orderCancelled_throwsValidationException() {
        testOrder.setStatus(OrderStatus.CANCELLED);
        CreatePaymentRequest request = new CreatePaymentRequest(USER_ID, ORDER_ID, new BigDecimal("1500.00"), "ref");

        when(orderService.findByIdHelper(eq(ORDER_ID), any())).thenReturn(testOrder);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> bkashPaymentService.create(request, null));

        assertEquals("Cannot pay for cancelled or rejected order", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw ValidationException when order is rejected")
    void create_orderRejected_throwsValidationException() {
        testOrder.setStatus(OrderStatus.REJECTED);
        CreatePaymentRequest request = new CreatePaymentRequest(USER_ID, ORDER_ID, new BigDecimal("1500.00"), "ref");

        when(orderService.findByIdHelper(eq(ORDER_ID), any())).thenReturn(testOrder);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> bkashPaymentService.create(request, null));

        assertEquals("Cannot pay for cancelled or rejected order", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw ValidationException when product has insufficient stock")
    void create_insufficientStock_throwsValidationException() {
        CreatePaymentRequest request = new CreatePaymentRequest(USER_ID, ORDER_ID, new BigDecimal("1500.00"), "ref");
        when(orderService.findByIdHelper(eq(ORDER_ID), any())).thenReturn(testOrder);
        doThrow(new ValidationException("Insufficient stock for product: Wireless Mouse"))
                .when(orderService).validateStockForPayment(testOrder);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> bkashPaymentService.create(request, null));

        assertTrue(ex.getMessage().contains("Insufficient stock"));
        assertTrue(ex.getMessage().contains("Wireless Mouse"));
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when non-owner non-admin tries to pay")
    void create_nonOwnerNonAdmin_throwsAccessDeniedException() {
        CreatePaymentRequest request = new CreatePaymentRequest(OTHER_USER_ID, ORDER_ID, new BigDecimal("1500.00"), "ref");

        CustomUserDetails otherUser = mock(CustomUserDetails.class);

        when(orderService.findByIdHelper(eq(ORDER_ID), any()))
                .thenThrow(new AccessDeniedException("Access denied"));

        assertThrows(AccessDeniedException.class,
                () -> bkashPaymentService.create(request, otherUser));
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
