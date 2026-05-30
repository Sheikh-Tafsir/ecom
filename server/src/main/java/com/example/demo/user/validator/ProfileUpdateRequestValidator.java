package com.example.demo.user.validator;

import com.example.demo.user.dto.UpdateProfileRequest;
import com.example.demo.common.validator.CommonValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class ProfileUpdateRequestValidator {

    private final CommonValidator commonValidator;

    public void validate(UpdateProfileRequest updateProfileRequest, Errors errors) {
        MultipartFile image = updateProfileRequest.image();
        commonValidator.validateImage(image, false, "image", errors);
    }
}
