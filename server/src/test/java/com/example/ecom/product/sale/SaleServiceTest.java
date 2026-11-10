package com.example.ecom.product.sale;

import com.example.ecom.order.model.Order;
import com.example.ecom.product.product.model.Product;
import com.example.ecom.product.sale.model.Sale;
import com.example.ecom.product.sale.dto.SaleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @InjectMocks
    private SaleService saleService;

    private static final UUID PRODUCT_ID = UUID.fromString("a1a1a1a1-b2b2-c3c3-d4d4-e5e5e5e5e5e5");
    private static final UUID ORDER_ID = UUID.fromString("b2b2b2b2-c3c3-d4d4-e5e5-f6f6f6f6f6f6");
    private static final UUID SALE_ID = UUID.fromString("c3c3c3c3-d4d4-e5e5-f6f6-a7a7a7a7a7a7");

    private Product product;
    private Order order;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(PRODUCT_ID);
        product.setName("Bluetooth Speaker");
        product.setPrice(new BigDecimal("49.99"));
        product.setImages(new HashSet<>());

        order = new Order();
        order.setId(ORDER_ID);
    }

    @Test
    @DisplayName("findAll should return paginated sale responses")
    void findAll_withFilters_returnsPaginatedSales() {
        Sale sale = new Sale();
        sale.setId(SALE_ID);
        sale.setProduct(product);
        sale.setOrder(order);
        sale.setQuantity(3);
        sale.setProfit(new BigDecimal("45.00"));
        sale.setCreatedAt(Instant.now());

        Pageable pageable = PageRequest.of(0, 10);
        Page<Sale> salePage = new PageImpl<>(List.of(sale), pageable, 1);

        when(saleRepository.findAllByMonth(any(Instant.class), any(Instant.class), eq(PRODUCT_ID), eq("Speaker"), any(Pageable.class)))
                .thenReturn(salePage);

        Page<SaleResponse> result = saleService.findAll(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                PRODUCT_ID, "Speaker", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(SALE_ID, result.getContent().get(0).getId());
        verify(saleRepository).findAllByMonth(any(Instant.class), any(Instant.class), eq(PRODUCT_ID), eq("Speaker"), any(Pageable.class));
    }

    @Test
    @DisplayName("findAll should return empty page when no sales match")
    void findAll_noMatchingSales_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Sale> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(saleRepository.findAllByMonth(any(Instant.class), any(Instant.class), any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<SaleResponse> result = saleService.findAll(null, null, null, null, pageable);

        assertEquals(0, result.getTotalElements());
        verify(saleRepository).findAllByMonth(any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("add should create a Sale object with correct fields and calculated profit")
    void add_validInputs_returnsSaleWithCorrectFields() {
        BigDecimal unitProfit = new BigDecimal("15.00");

        Sale result = saleService.add(product, order, 3, unitProfit);

        assertNotNull(result);
        assertEquals(product, result.getProduct());
        assertEquals(order, result.getOrder());
        assertEquals(3, result.getQuantity());
        assertEquals(new BigDecimal("45.00"), result.getProfit());
    }

    @Test
    @DisplayName("add should compute profit as unit profit multiplied by quantity")
    void add_singleQuantity_profitEqualsUnitProfit() {
        BigDecimal unitProfit = new BigDecimal("10.50");

        Sale result = saleService.add(product, order, 1, unitProfit);

        assertEquals(new BigDecimal("10.50"), result.getProfit());
    }

    @Test
    @DisplayName("add should handle zero quantity correctly")
    void add_zeroQuantity_profitIsZero() {
        BigDecimal unitProfit = new BigDecimal("15.00");

        Sale result = saleService.add(product, order, 0, unitProfit);

        assertEquals(new BigDecimal("0.00"), result.getProfit());
        assertEquals(0, result.getQuantity());
    }

    @Test
    @DisplayName("add should handle negative unit profit (loss scenario)")
    void add_negativeUnitProfit_computesNegativeProfit() {
        BigDecimal unitProfit = new BigDecimal("-5.00");

        Sale result = saleService.add(product, order, 2, unitProfit);

        assertEquals(new BigDecimal("-10.00"), result.getProfit());
    }

    @Test
    @DisplayName("createAll should save all sales via repository")
    void createAll_multipleSales_savesAll() {
        Sale sale1 = new Sale();
        sale1.setQuantity(1);
        Sale sale2 = new Sale();
        sale2.setQuantity(2);

        saleService.createAll(List.of(sale1, sale2));

        verify(saleRepository).saveAll(List.of(sale1, sale2));
    }

    @Test
    @DisplayName("createAll should handle empty list")
    void createAll_emptyList_savesEmptyList() {
        saleService.createAll(List.of());

        verify(saleRepository).saveAll(List.of());
    }
}
