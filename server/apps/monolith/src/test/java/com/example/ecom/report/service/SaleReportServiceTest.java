package com.example.ecom.report.service;

import com.example.ecom.common.enums.AppModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleReportServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private SaleReportService saleReportService;

    @BeforeEach
    void setUp() {
        saleReportService = new SaleReportService(jdbcTemplate);
    }

    @Test
    @DisplayName("Should return SALE module")
    void getModule_always_returnsSaleModule() {
        assertEquals(AppModule.SALE, saleReportService.getModule());
    }

    @Test
    @DisplayName("Should return correct sale report headers")
    void getHeaders_always_returnsExpectedHeaders() {
        List<String> headers = saleReportService.getHeaders();

        assertEquals(6, headers.size());
        assertEquals("Sale ID", headers.get(0));
        assertEquals("Product ID", headers.get(1));
        assertEquals("Order ID", headers.get(2));
        assertEquals("Quantity", headers.get(3));
        assertEquals("Profit", headers.get(4));
        assertEquals("Created At", headers.get(5));
    }

    @Test
    @DisplayName("Should return base SQL selecting from sales table")
    void getBaseSql_always_containsSalesTable() {
        String sql = saleReportService.getBaseSql();

        assertNotNull(sql);
        assertTrue(sql.contains("FROM sales"));
        assertTrue(sql.contains("product_id"));
        assertTrue(sql.contains("order_id"));
        assertTrue(sql.contains("profit"));
    }

    @Test
    @DisplayName("Should map ResultSet row to RowConsumer with correct values")
    void mapRow_validResultSet_passesValuesToConsumer() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("id")).thenReturn("sale-uuid-001");
        when(rs.getString("product_id")).thenReturn("prod-uuid-002");
        when(rs.getString("order_id")).thenReturn("ord-uuid-003");
        when(rs.getInt("quantity")).thenReturn(10);
        when(rs.getBigDecimal("profit")).thenReturn(new BigDecimal("350.75"));

        Timestamp ts = Timestamp.from(Instant.parse("2026-03-20T14:00:00Z"));
        when(rs.getTimestamp("created_at")).thenReturn(ts);

        Object[][] captured = new Object[1][];
        RowConsumer consumer = values -> captured[0] = values;

        saleReportService.mapRow(rs, consumer);

        assertNotNull(captured[0]);
        assertEquals(6, captured[0].length);
        assertEquals("sale-uuid-001", captured[0][0]);
        assertEquals("prod-uuid-002", captured[0][1]);
        assertEquals("ord-uuid-003", captured[0][2]);
        assertEquals(10, captured[0][3]);
        assertEquals(new BigDecimal("350.75"), captured[0][4]);
        assertEquals(ts, captured[0][5]);

        verify(rs).getString("id");
        verify(rs).getInt("quantity");
        verify(rs).getBigDecimal("profit");
    }

    @Test
    @DisplayName("Should generate filename containing 'sale' and '.csv' extension")
    void getFileName_always_containsSaleAndCsvExtension() {
        String fileName = saleReportService.getFileName();

        assertNotNull(fileName);
        assertTrue(fileName.contains("sale"));
        assertTrue(fileName.endsWith(".csv"));
        assertTrue(fileName.startsWith("Ecom_"));
    }
}
