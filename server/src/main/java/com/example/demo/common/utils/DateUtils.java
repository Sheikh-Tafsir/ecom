package com.example.demo.common.utils;

import java.time.LocalDate;

public final class DateUtils {

    private DateUtils() {}

    public static String getMonthFromValue(Integer id) {
        if (id == 1) return "January";
        else if (id == 2) return "February";
        else if (id == 3) return "March";
        else if (id == 4) return "April";
        else if (id == 5) return "May";
        else if (id == 6) return "June";
        else if (id == 7) return "July";
        else if (id == 8) return "August";
        else if (id == 9) return "September";
        else if (id == 10) return "October";
        else if (id == 11) return "November";
        else if (id == 12) return "December";
        else throw new IllegalArgumentException("Invalid month");
    }

    public static int getCurrentYear() {
        LocalDate today = LocalDate.now();
        return today.getYear();
    }
}
