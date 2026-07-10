package com.example.demo.report.controller;

import com.example.demo.report.dto.ReportCreateRequest;
import com.example.demo.report.service.ReportService;
import com.example.demo.report.service.ReportServiceFactory;
import com.example.demo.report.validator.ReportValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import static com.example.demo.common.utils.Utils.checkErrors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reports")
public class ReportController {

    private final ReportValidator reportValidator;

    private final ReportServiceFactory reportServiceFactory;

    @PostMapping
    @PreAuthorize("hasAnyAuthority(T(com.example.demo.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    public ResponseEntity<StreamingResponseBody> create(@Valid @RequestBody ReportCreateRequest request,
                                                        BindingResult bindingResult) {

        reportValidator.validate(request, bindingResult);
        checkErrors(bindingResult);

        ReportService reportService = reportServiceFactory.getService(request.getModule());

        StreamingResponseBody responseBody = outputStream -> reportService.writeCsv(outputStream, request);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + reportService.getFileName() + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(responseBody);
    }
}
