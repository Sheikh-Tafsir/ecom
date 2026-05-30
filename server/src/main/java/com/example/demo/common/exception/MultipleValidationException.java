package com.example.demo.common.exception;

import lombok.Getter;
import org.springframework.validation.BindingResult;

@Getter
public class MultipleValidationException extends RuntimeException {

    private final BindingResult bindingResult;

    public MultipleValidationException(BindingResult bindingResult) {
        super("Validation failed");
        this.bindingResult = bindingResult;
    }
}
