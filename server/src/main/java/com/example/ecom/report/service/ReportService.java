package com.example.ecom.report.service;

import com.example.ecom.common.enums.AppModule;
import com.example.ecom.report.dto.ReportCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class ReportService {

    protected final JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter FILENAME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneOffset.UTC);

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
        String timestamp = FILENAME_FORMATTER.format(Instant.now());

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

        // Neutralize CSV / Spreadsheet formula injection (=, +, -, @, tabs)
        if (s.startsWith("=") || s.startsWith("+") || s.startsWith("-") || s.startsWith("@") || s.startsWith("\t")) {
            s = "\t" + s;
        }

        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
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
                params.add(Timestamp.from(fromDate.atStartOfDay(ZoneOffset.UTC).toInstant()));
            }

            if (toDate != null) {
                if (fromDate != null) {
                    sql.append(" AND ");
                }
                sql.append("created_at < ?");
                params.add(Timestamp.from(toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
            }
        }
        return params;
    }

    protected abstract AppModule getModule();

    protected abstract List<String> getHeaders();

    protected abstract String getBaseSql();

    protected abstract void mapRow(ResultSet rs, RowConsumer consumer) throws SQLException;
}
