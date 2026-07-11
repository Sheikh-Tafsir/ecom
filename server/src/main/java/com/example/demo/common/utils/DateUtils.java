package com.example.demo.common.utils;

import com.example.demo.common.dto.DateRangeDto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class DateUtils {

    private DateUtils() {}

    public static int getCurrentYear() {
        LocalDate today = LocalDate.now();
        return today.getYear();
    }

    public static DateRangeDto resolveDates(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime endDateTime = toDate != null
                ? toDate.atTime(23, 59, 59, 999_999_999)
                : LocalDateTime.now()
                .withHour(23)
                .withMinute(59)
                .withSecond(59)
                .withNano(999_999_999);

        LocalDateTime startDateTime = fromDate != null
                ? fromDate.atStartOfDay()
                : endDateTime.toLocalDate()
                .withMonth(1)
                .withDayOfMonth(1)
                .atStartOfDay();

        return new DateRangeDto(startDateTime, endDateTime);
    }
}
