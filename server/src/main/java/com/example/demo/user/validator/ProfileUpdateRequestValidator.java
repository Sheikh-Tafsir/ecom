package com.example.demo.user.validator;

import com.example.demo.user.dto.UpdateProfileRequest;
import com.example.demo.common.validator.CommonValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class ProfileUpdateRequestValidator implements Validator {

    private final CommonValidator commonValidator;

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz.equals(UpdateProfileRequest.class);
    }

    @Override
    public void validate(@NonNull Object target, @NonNull Errors errors) {
        UpdateProfileRequest updateProfileRequest = (UpdateProfileRequest) target;

        MultipartFile image = updateProfileRequest.image();
        commonValidator.validateImage(image, false, "image", errors);
    }
}
