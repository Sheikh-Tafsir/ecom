package com.example.demo.report.service;

import com.example.demo.common.enums.AppModule;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class UserReportService extends ReportService {

    private static final List<String> HEADERS = List.of(
            "User ID",
            "Name",
            "Email",
            "Phone",
            "Status"
    );

    private static final String SQL = """
            SELECT
                id,
                name,
                email,
                phone,
                status
            FROM users
            """;

    public UserReportService(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public AppModule getModule() {
        return AppModule.USER;
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
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("status")
        );
    }

}
