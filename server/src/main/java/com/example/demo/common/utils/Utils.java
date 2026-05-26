package com.example.demo.common.utils;

import org.springframework.util.StringUtils;

import java.util.Random;

public final class Utils {

    private Utils() {
    }

    public static boolean isNull(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static String generatePassword(String name) {
        String firstPart = name.substring(0, 3);
        String lastPart = name.substring(name.length() - 3);

        Random random = new Random();
        int randomNum = 10 + random.nextInt(90);

        return firstPart + lastPart + randomNum;
    }

    public static <T extends Enum<T>> T getEnumRequired(Class<T> enumClass, String value, String enumName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(enumName + " is null");
        }

        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid " + enumName + ": " + value);
        }
    }

    public static <T extends Enum<T>> T getEnumNonRequired(Class<T> enumClass, String value, String enumName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid " + enumName + ": " + value);
        }
    }

    public static String getModelListName(String modelName) {
        return modelName + " list";
    }
}
