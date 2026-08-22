package com.example.ecom.common.utils;

import com.example.ecom.common.dto.DateRangeDto;

import java.time.*;

public final class DateUtils {

    private DateUtils() {}

    public static int getCurrentYear() {
        return LocalDate.now().getYear();
    }

    public static DateRangeDto resolveDates(LocalDate fromDate, LocalDate toDate) {
        ZoneOffset offset = ZoneOffset.UTC;

        Instant endDateTime = toDate != null
                ? toDate.atTime(LocalTime.MAX).toInstant(offset)
                : ZonedDateTime.now(offset)
                .with(LocalTime.MAX)
                .toInstant();

        Instant startDateTime = fromDate != null
                ? fromDate.atStartOfDay(offset).toInstant()
                : ZonedDateTime.ofInstant(endDateTime, offset)
                .withMonth(1)
                .withDayOfMonth(1)
                .with(LocalTime.MIN)
                .toInstant();

        return new DateRangeDto(startDateTime, endDateTime);
    }
}
