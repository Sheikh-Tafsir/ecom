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
class OrderReportServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private OrderReportService orderReportService;

    @BeforeEach
    void setUp() {
        orderReportService = new OrderReportService(jdbcTemplate);
    }

    @Test
    @DisplayName("Should return ORDER module")
    void getModule_always_returnsOrderModule() {
        assertEquals(AppModule.ORDER, orderReportService.getModule());
    }

    @Test
    @DisplayName("Should return correct order report headers")
    void getHeaders_always_returnsExpectedHeaders() {
        List<String> headers = orderReportService.getHeaders();

        assertEquals(9, headers.size());
        assertEquals("Order ID", headers.get(0));
        assertEquals("User ID", headers.get(1));
        assertEquals("Total Price", headers.get(2));
        assertEquals("Status", headers.get(3));
        assertEquals("Name", headers.get(4));
        assertEquals("Phone", headers.get(5));
        assertEquals("Address", headers.get(6));
        assertEquals("Payment Method", headers.get(7));
        assertEquals("Created At", headers.get(8));
    }

    @Test
    @DisplayName("Should return base SQL selecting from orders table")
    void getBaseSql_always_containsOrdersTable() {
        String sql = orderReportService.getBaseSql();

        assertNotNull(sql);
        assertTrue(sql.contains("FROM orders"));
        assertTrue(sql.contains("id"));
        assertTrue(sql.contains("total_price"));
        assertTrue(sql.contains("status"));
    }

    @Test
    @DisplayName("Should map ResultSet row to RowConsumer with correct values")
    void mapRow_validResultSet_passesValuesToConsumer() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("id")).thenReturn("ord-uuid-001");
        when(rs.getString("user_id")).thenReturn("user-uuid-002");
        when(rs.getBigDecimal("total_price")).thenReturn(new BigDecimal("2500.50"));
        when(rs.getString("status")).thenReturn("PENDING");
        when(rs.getString("name")).thenReturn("Rafiq Ahmed");
        when(rs.getString("phone")).thenReturn("01712345678");
        when(rs.getString("address")).thenReturn("Dhaka, Bangladesh");
        when(rs.getString("payment_method")).thenReturn("CASH_ON_DELIVERY");

        Timestamp ts = Timestamp.from(Instant.parse("2026-01-15T10:30:00Z"));
        when(rs.getTimestamp("created_at")).thenReturn(ts);

        Object[][] captured = new Object[1][];
        RowConsumer consumer = values -> captured[0] = values;

        orderReportService.mapRow(rs, consumer);

        assertNotNull(captured[0]);
        assertEquals(9, captured[0].length);
        assertEquals("ord-uuid-001", captured[0][0]);
        assertEquals("user-uuid-002", captured[0][1]);
        assertEquals(new BigDecimal("2500.50"), captured[0][2]);
        assertEquals("PENDING", captured[0][3]);
        assertEquals("Rafiq Ahmed", captured[0][4]);
        assertEquals("01712345678", captured[0][5]);
        assertEquals("Dhaka, Bangladesh", captured[0][6]);
        assertEquals("CASH_ON_DELIVERY", captured[0][7]);
        assertEquals(ts, captured[0][8]);

        verify(rs).getString("id");
        verify(rs).getString("user_id");
        verify(rs).getBigDecimal("total_price");
    }

    @Test
    @DisplayName("Should generate filename containing 'order' and '.csv' extension")
    void getFileName_always_containsOrderAndCsvExtension() {
        String fileName = orderReportService.getFileName();

        assertNotNull(fileName);
        assertTrue(fileName.contains("order"));
        assertTrue(fileName.endsWith(".csv"));
        assertTrue(fileName.startsWith("Ecom_"));
    }
}
