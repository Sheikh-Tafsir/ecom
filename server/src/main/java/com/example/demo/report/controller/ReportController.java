package com.example.demo.report.controller;

import com.example.demo.report.dto.ReportCreateRequest;
import com.example.demo.report.service.ReportService;
import com.example.demo.report.service.ReportServiceFactory;
import com.example.demo.report.validator.ReportValidator;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

import static com.example.demo.common.utils.Utils.checkErrors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reports")
public class ReportController {

    private final ReportValidator reportValidator;

    private final ReportServiceFactory reportServiceFactory;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public void create(@Valid @RequestBody ReportCreateRequest request,
                       BindingResult bindingResult,
                       HttpServletResponse response) throws IOException {

        reportValidator.validate(request, bindingResult);
        checkErrors(bindingResult);

        ReportService reportService = reportServiceFactory.getService(request.getModule());
        reportService.generateFile(request, response);
    }
}
