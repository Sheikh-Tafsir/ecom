package com.example.ecom.report.dto;

import com.example.ecom.common.enums.AppModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReportCreateRequestTest {

    @Test
    @DisplayName("All-args constructor sets all fields")
    void allArgsConstructor_validArgs_setsFields() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 6, 30);

        ReportCreateRequest request = new ReportCreateRequest(AppModule.ORDER, from, to);

        assertEquals(AppModule.ORDER, request.getModule());
        assertEquals(from, request.getFromDate());
        assertEquals(to, request.getToDate());
    }

    @Test
    @DisplayName("No-args constructor creates empty instance")
    void noArgsConstructor_createsEmpty() {
        ReportCreateRequest request = new ReportCreateRequest();
        assertNull(request.getModule());
        assertNull(request.getFromDate());
        assertNull(request.getToDate());
    }

    @Test
    @DisplayName("Setters update all fields")
    void setters_allFields_updateCorrectly() {
        ReportCreateRequest request = new ReportCreateRequest();
        request.setModule(AppModule.SALE);
        request.setFromDate(LocalDate.of(2025, 3, 1));
        request.setToDate(LocalDate.of(2025, 3, 31));

        assertEquals(AppModule.SALE, request.getModule());
        assertEquals(LocalDate.of(2025, 3, 1), request.getFromDate());
        assertEquals(LocalDate.of(2025, 3, 31), request.getToDate());
    }

    @Test
    @DisplayName("All AppModule values are accepted")
    void module_allValues_accepted() {
        for (AppModule module : AppModule.values()) {
            ReportCreateRequest request = new ReportCreateRequest(module, null, null);
            assertEquals(module, request.getModule());
        }
    }
}
