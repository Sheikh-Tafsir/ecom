package com.example.demo.report.service;

import com.example.demo.common.enums.AppModule;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class SaleReportService extends ReportService {

    private static final List<String> HEADERS = List.of(
            "Sale ID",
            "Product ID",
            "Order ID",
            "Quantity",
            "Profit",
            "Created At"
    );

    private static final String SQL = """
            SELECT
                id,
                product_id,
                order_id,
                quantity,
                profit,
                created_at
            FROM sales
            """;

    public SaleReportService(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public AppModule getModule() {
        return AppModule.SALE;
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
                rs.getLong("product_id"),
                rs.getLong("order_id"),
                rs.getInt("quantity"),
                rs.getBigDecimal("profit"),
                rs.getTimestamp("created_at")
        );
    }
}
