package com.example.demo.common.exception;

import lombok.Getter;
import org.springframework.validation.BindingResult;

@Getter
public class JsrValidationException extends RuntimeException {

    private final BindingResult bindingResult;

    public JsrValidationException(BindingResult bindingResult) {
        super("Validation failed");
        this.bindingResult = bindingResult;
    }
}
