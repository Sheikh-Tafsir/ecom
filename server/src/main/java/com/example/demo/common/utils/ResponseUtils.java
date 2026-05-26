package com.example.demo.common.utils;

import com.example.demo.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ResponseUtils {

    private static final String GLOBAL_ERROR = "global";

    private ResponseUtils() {
        throw new RuntimeException("Cannot have a instance");
    }

    // Success
    public static <T> ResponseEntity<ApiResponse<T>> ok(String message) {
        return ResponseUtils.ok(null, message);
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
        return new ResponseEntity<>(new ApiResponse<>(data, message), HttpStatus.OK);
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(String message) {
        return ResponseUtils.created(null, message);
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
        return new ResponseEntity<>(new ApiResponse<>(data, message), HttpStatus.CREATED);
    }

    // Errors
    public static ResponseEntity<ApiResponse<Void>> error(String message, HttpStatus status) {
        return error(getListMap(message), status);
    }

    public static ResponseEntity<ApiResponse<Void>> error(Map<String, List<String>> errors, HttpStatus status) {
        return new ResponseEntity<>(new ApiResponse<>(errors), status);
    }

    public static ResponseEntity<ApiResponse<Void>> error(String message, HttpStatusCode statusCode) {
        return error(getListMap(message), statusCode);
    }

    public static ResponseEntity<ApiResponse<Void>> error(Map<String, List<String>> errors, HttpStatusCode statusCode) {
        return new ResponseEntity<>(new ApiResponse<>(errors), statusCode);
    }

    private static Map<String, List<String>> getListMap(String message) {
        Map<String, List<String>> errors = new HashMap<>();
        errors.put(GLOBAL_ERROR, List.of(message));
        return errors;
    }

    public static Map<String, List<String>> getErrorsFromMethodArgNotValidException(MethodArgumentNotValidException ex) {
        Map<String, List<String>> errors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.computeIfAbsent(error.getField(), key -> new ArrayList<>())
                    .add(error.getDefaultMessage());
        }

        for (ObjectError objectError : ex.getBindingResult().getGlobalErrors()) {
            errors.computeIfAbsent(objectError.getObjectName(), key -> new ArrayList<>())
                    .add(objectError.getDefaultMessage());
        }

        return errors;
    }
}