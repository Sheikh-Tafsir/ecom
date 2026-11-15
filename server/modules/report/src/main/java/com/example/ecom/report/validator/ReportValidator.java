package com.example.ecom.report.validator;

import com.example.ecom.report.dto.ReportCreateRequest;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

@Component
public class ReportValidator {

    public void validate(ReportCreateRequest request, Errors errors) {
        if (request.getFromDate() != null && request.getToDate() != null
                && request.getFromDate().isAfter(request.getToDate())) {
            errors.reject("error.fromDate.after.toDate", "From date cannot be after to date");
        }
    }
}
