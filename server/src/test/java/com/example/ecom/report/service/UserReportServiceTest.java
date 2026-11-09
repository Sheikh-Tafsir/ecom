package com.example.ecom.report.service;

import com.example.ecom.common.enums.AppModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserReportServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private UserReportService userReportService;

    @BeforeEach
    void setUp() {
        userReportService = new UserReportService(jdbcTemplate);
    }

    @Test
    @DisplayName("Should return USER module")
    void getModule_always_returnsUserModule() {
        assertEquals(AppModule.USER, userReportService.getModule());
    }

    @Test
    @DisplayName("Should return correct user report headers")
    void getHeaders_always_returnsExpectedHeaders() {
        List<String> headers = userReportService.getHeaders();

        assertEquals(4, headers.size());
        assertEquals("User ID", headers.get(0));
        assertEquals("Name", headers.get(1));
        assertEquals("Email", headers.get(2));
        assertEquals("Status", headers.get(3));
    }

    @Test
    @DisplayName("Should return base SQL selecting from users table")
    void getBaseSql_always_containsUsersTable() {
        String sql = userReportService.getBaseSql();

        assertNotNull(sql);
        assertTrue(sql.contains("FROM users"));
        assertTrue(sql.contains("name"));
        assertTrue(sql.contains("email"));
        assertTrue(sql.contains("status"));
    }

    @Test
    @DisplayName("Should map ResultSet row to RowConsumer with correct values")
    void mapRow_validResultSet_passesValuesToConsumer() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("id")).thenReturn("user-uuid-001");
        when(rs.getString("name")).thenReturn("Karim Hossain");
        when(rs.getString("email")).thenReturn("karim@example.com");
        when(rs.getString("status")).thenReturn("ACTIVE");

        Object[][] captured = new Object[1][];
        RowConsumer consumer = values -> captured[0] = values;

        userReportService.mapRow(rs, consumer);

        assertNotNull(captured[0]);
        assertEquals(4, captured[0].length);
        assertEquals("user-uuid-001", captured[0][0]);
        assertEquals("Karim Hossain", captured[0][1]);
        assertEquals("karim@example.com", captured[0][2]);
        assertEquals("ACTIVE", captured[0][3]);

        verify(rs).getString("id");
        verify(rs).getString("name");
        verify(rs).getString("email");
        verify(rs).getString("status");
    }

    @Test
    @DisplayName("Should generate filename containing 'user' and '.csv' extension")
    void getFileName_always_containsUserAndCsvExtension() {
        String fileName = userReportService.getFileName();

        assertNotNull(fileName);
        assertTrue(fileName.contains("user"));
        assertTrue(fileName.endsWith(".csv"));
        assertTrue(fileName.startsWith("Ecom_"));
    }
}
