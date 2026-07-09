package com.example.demo.report.service;

import com.example.demo.common.enums.AppModule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ReportServiceFactory {

    private final Map<AppModule, ReportService> services;

    public ReportServiceFactory(List<ReportService> services) {
        this.services = services.stream()
                .collect(Collectors.toMap(
                        ReportService::getModule,
                        Function.identity()
                ));
    }

    public ReportService getService(AppModule module) {
        return services.get(module);
    }
}
