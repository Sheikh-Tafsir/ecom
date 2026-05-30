package com.example.demo.auth.validator;

import com.example.demo.auth.dto.SignupRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class AuthValidator {

    public void validateSignup(SignupRequest signupRequest, Errors errors) {
        validatePasswords(signupRequest.password(), signupRequest.confirmPassword(), errors);
    }

    public void validatePasswords(String password, String confirmPassword, Errors errors) {
        if (StringUtils.hasText(password) && StringUtils.hasText(confirmPassword) && !confirmPassword.equals(password)) {
            errors.rejectValue("newPassword", "error.password.confirm.password.not.match");
        }
    }
}
