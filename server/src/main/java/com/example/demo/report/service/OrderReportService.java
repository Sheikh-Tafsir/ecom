package com.example.demo.report.service;

import com.example.demo.common.enums.AppModule;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class OrderReportService extends ReportService {

    private static final List<String> HEADERS = List.of(
            "Order ID",
            "User ID",
            "Total Price",
            "Status",
            "Name",
            "Phone",
            "Address",
            "Payment Method",
            "Created At"
    );

    private static final String SQL = """
            SELECT
                id,
                user_id,
                total_price,
                status,
                name,
                phone,
                address,
                payment_method,
                created_at
            FROM orders
            """;

    public OrderReportService(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public AppModule getModule() {
        return AppModule.ORDER;
    }

    @Override
    public List<String> getHeaders() {
        return HEADERS;
    }

    @Override
    public String getBaseSql() {
        return SQL;
    }

    @Override
    public void mapRow(ResultSet rs, RowConsumer consumer) throws SQLException {
        consumer.accept(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getBigDecimal("total_price"),
                rs.getString("status"),
                rs.getString("name"),
                rs.getString("phone"),
                rs.getString("address"),
                rs.getString("payment_method"),
                rs.getTimestamp("created_at")
        );
    }
}
