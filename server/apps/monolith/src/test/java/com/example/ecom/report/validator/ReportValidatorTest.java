package com.example.ecom.report.validator;

import com.example.ecom.common.enums.AppModule;
import com.example.ecom.report.dto.ReportCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReportValidatorTest {

    @InjectMocks
    private ReportValidator reportValidator;

    private ReportCreateRequest request;
    private Errors errors;

    @BeforeEach
    void setUp() {
        request = new ReportCreateRequest();
        request.setModule(AppModule.ORDER);
        errors = new BeanPropertyBindingResult(request, "reportCreateRequest");
    }

    @Test
    @DisplayName("Should not reject when fromDate is before toDate")
    void validate_fromDateBeforeToDate_noErrors() {
        request.setFromDate(LocalDate.of(2026, 1, 1));
        request.setToDate(LocalDate.of(2026, 6, 30));

        reportValidator.validate(request, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("Should not reject when fromDate equals toDate")
    void validate_fromDateEqualsToDate_noErrors() {
        LocalDate date = LocalDate.of(2026, 5, 15);
        request.setFromDate(date);
        request.setToDate(date);

        reportValidator.validate(request, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("Should reject when fromDate is after toDate")
    void validate_fromDateAfterToDate_rejectsWithError() {
        request.setFromDate(LocalDate.of(2026, 12, 31));
        request.setToDate(LocalDate.of(2026, 1, 1));

        reportValidator.validate(request, errors);

        assertTrue(errors.hasErrors());
        assertTrue(errors.hasGlobalErrors());
        assertEquals("error.fromDate.after.toDate", errors.getGlobalErrors().get(0).getCode());
        assertEquals("From date cannot be after to date", errors.getGlobalErrors().get(0).getDefaultMessage());
    }

    @Test
    @DisplayName("Should not reject when both dates are null")
    void validate_bothDatesNull_noErrors() {
        request.setFromDate(null);
        request.setToDate(null);

        reportValidator.validate(request, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("Should not reject when only fromDate is provided")
    void validate_onlyFromDate_noErrors() {
        request.setFromDate(LocalDate.of(2026, 3, 1));
        request.setToDate(null);

        reportValidator.validate(request, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("Should not reject when only toDate is provided")
    void validate_onlyToDate_noErrors() {
        request.setFromDate(null);
        request.setToDate(LocalDate.of(2026, 9, 30));

        reportValidator.validate(request, errors);

        assertFalse(errors.hasErrors());
    }
}
