package com.example.demo.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse <T> {

    private T data;

    private String message;

    private Map<String, List<String>> errors;

    // Success response
    public ApiResponse(T data, String message) {
        this.data = data;
        this.message = message;
    }

    // Error response
    public ApiResponse(Map<String, List<String>> errors) {
        this.errors = errors;
    }
}
