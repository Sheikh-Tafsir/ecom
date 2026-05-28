package com.example.demo.common.helper;

import com.example.demo.common.exception.JsrValidationException;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

@Component
public class CommonHelper {

    public void checkErrors(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new JsrValidationException(bindingResult);
        }
    }
}
