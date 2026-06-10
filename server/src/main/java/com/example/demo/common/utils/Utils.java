package com.example.demo.common.utils;

import com.example.demo.common.serializer.StringTrimmerDeserializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Random;

public final class Utils {

    public static final int MAX_PAGE_SIZE = 24;

    public static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = JsonMapper.builder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .addModule(new SimpleModule("TrimStringsModule")
                        .addDeserializer(String.class, new StringTrimmerDeserializer()))
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
    }

    private Utils() {
    }

    public static String objectHash(Object request) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(request);
            return DigestUtils.sha256Hex(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isNull(String s) {
        return s == null || s.trim().isEmpty();
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

    public static boolean isEmpty(Collection<?> c) {
        return c == null || c.isEmpty();
    }

    public static <T> Collection<T> nullSafeCollection(Collection<T> c) {
        return c == null ? Collections.emptyList() : c;
    }

    public static Pageable getValidPageable(Pageable pageable) {
        return getValidPageable(pageable, MAX_PAGE_SIZE);
    }

    public static Pageable getValidPageable(Pageable pageable, int maxSize) {
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, maxSize);
        }

        return PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), maxSize), pageable.getSort());
    }
}
