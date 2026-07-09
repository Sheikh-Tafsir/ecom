package com.example.demo.report.dto;

import com.example.demo.common.enums.AppModule;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportCreateRequest {

    @NotNull
    private AppModule module;

    @PastOrPresent
    private LocalDate fromDate;

    @PastOrPresent
    private LocalDate toDate;
}
