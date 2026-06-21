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

    public static DateRangeDto resolveDates(LocalDateTime fromDate, LocalDateTime toDate) {
        toDate = toDate != null
                ? toDate
                : LocalDateTime.now()
                .withHour(23)
                .withMinute(59)
                .withSecond(59)
                .withNano(999_999_999);

        fromDate = fromDate != null
                ? fromDate
                : toDate.toLocalDate()
                .withMonth(1)
                .withDayOfMonth(1)
                .atStartOfDay();

        return new DateRangeDto(fromDate, toDate);
    }
}
