package com.example.demo.report.service;

import com.example.demo.common.enums.AppModule;
import com.example.demo.report.dto.ReportCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class ReportService {

    protected final JdbcTemplate jdbcTemplate;

    public void writeCsv(OutputStream outputStream, ReportCreateRequest request) {
        PrintWriter writer = new PrintWriter(outputStream);

        writer.println(String.join(",", getHeaders()));

        streamRows(
                values -> {
                    String row = Arrays.stream(values)
                            .map(this::formatCsvValue)
                            .collect(Collectors.joining(","));
                    writer.println(row);
                },
                request.getFromDate(),
                request.getToDate()
        );

        writer.flush();
    }

    public String getFileName() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        return "Ecom_" + getModule().getValue().toLowerCase() + "_report_" + timestamp + ".csv";
    }

    private void streamRows(RowConsumer consumer,
                            LocalDate fromDate,
                            LocalDate toDate) {

        StringBuilder sql = new StringBuilder(getBaseSql());

        List<Object> params = addFilters(fromDate, toDate, sql);

        jdbcTemplate.query(
                sql.toString(),
                params.toArray(),
                rs -> {
                    while (rs.next()) {
                        mapRow(rs, consumer);
                    }
                }
        );
    }

    private String formatCsvValue(Object v) {
        if (v == null) return "";
        String s = v.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static List<Object> addFilters(LocalDate fromDate, LocalDate toDate, StringBuilder sql) {
        List<Object> params = new ArrayList<>();

        if (fromDate != null || toDate != null) {
            sql.append(" WHERE ");

            if (fromDate != null) {
                sql.append("created_at >= ?");
                params.add(fromDate.atStartOfDay());
            }

            if (toDate != null) {
                if (fromDate != null) {
                    sql.append(" AND ");
                }
                sql.append("created_at < ?");
                params.add(toDate.plusDays(1).atStartOfDay());
            }
        }
        return params;
    }

    protected abstract AppModule getModule();

    protected abstract List<String> getHeaders();

    protected abstract String getBaseSql();

    protected abstract void mapRow(ResultSet rs, RowConsumer consumer) throws SQLException;
}
