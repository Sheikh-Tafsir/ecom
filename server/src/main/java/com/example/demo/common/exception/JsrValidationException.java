package com.example.demo.common.exception;

import lombok.Getter;
import org.springframework.validation.BindingResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class JsrValidationException extends RuntimeException {
    private final Map<String, List<String>> errors;

    public JsrValidationException(BindingResult bindingResult) {
        super("Validation failed");
        this.errors = new LinkedHashMap<>();

        bindingResult.getFieldErrors().forEach(f ->
                this.errors.computeIfAbsent(f.getField(), key -> new ArrayList<>()).add(f.getDefaultMessage())
        );

        bindingResult.getGlobalErrors().forEach(g ->
                this.errors.computeIfAbsent(g.getObjectName(), key -> new ArrayList<>()).add(g.getDefaultMessage())
        );
    }
}
