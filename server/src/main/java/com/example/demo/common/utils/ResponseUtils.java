package com.example.demo.common.utils;

import com.example.demo.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ResponseUtils {

    private static final String GLOBAL_ERROR = "global";

    private ResponseUtils() {
    }

    // Success ok
    public static <T> ResponseEntity<ApiResponse<T>> ok(String message) {
        return ResponseUtils.ok(null, message);
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
        return new ResponseEntity<>(new ApiResponse<>(data, message), HttpStatus.OK);
    }

    // Success created
    public static <T> ResponseEntity<ApiResponse<T>> created(String message) {
        return ResponseUtils.created(null, message);
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
        return new ResponseEntity<>(new ApiResponse<>(data, message), HttpStatus.CREATED);
    }

    // Errors
    public static ResponseEntity<ApiResponse<Void>> error(String message, HttpStatusCode statusCode) {
        Map<String, List<String>> errors = new HashMap<>();
        errors.put(GLOBAL_ERROR, List.of(message));

        return error(errors, statusCode);
    }

    public static ResponseEntity<ApiResponse<Void>> error(BindingResult bindingResult) {
        Map<String, List<String>> errors = new HashMap<>();

        for (FieldError error : bindingResult.getFieldErrors()) {
            errors.computeIfAbsent(error.getField(), key -> new ArrayList<>())
                    .add(error.getDefaultMessage());
        }

        for (ObjectError objectError : bindingResult.getGlobalErrors()) {
            errors.computeIfAbsent(GLOBAL_ERROR, key -> new ArrayList<>())
                    .add(objectError.getDefaultMessage());
        }

        return error(errors, HttpStatusCode.valueOf(422));
    }

    public static ResponseEntity<ApiResponse<Void>> error(Map<String, List<String>> errors, HttpStatusCode statusCode) {
        return new ResponseEntity<>(new ApiResponse<>(errors), statusCode);
    }
}