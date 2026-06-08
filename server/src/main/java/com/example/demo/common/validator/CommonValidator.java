package com.example.demo.common.validator;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

import static com.example.demo.common.utils.FileUtils.*;

@Component
public class CommonValidator {

    private static final String IMAGE_TYPE = "image/";

    public void validateString(String value, boolean required, int min, int max, String fieldName, Errors errors) {
        if (required && !StringUtils.hasText(value)) {
            errors.rejectValue(fieldName, "error.field.is.required");
            return;
        }

        if (value.length() < min || value.length() > max) {
            errors.rejectValue(fieldName, "error.field.size.outOfRange", new Object[]{min, max},
                    "Should be between " + min + " to " + max + " characters");
        }
    }

    public void validateImage(MultipartFile image, boolean required, String fieldName, Errors errors) {
        validateFile(image, required, fieldName, IMAGE_TYPE, errors);
    }

    private void validateFile(MultipartFile file, boolean required, String fieldName, String requiredType, Errors errors) {
        if (!fileExists(file)) {
            if (required) {
                errors.rejectValue(fieldName, "error.field.is.required");
            }

            return;
        }

        if (fileTooLarge(file)) {
            errors.rejectValue(fieldName, "error.file.tooLarge");
            return;
        }

        String contentType;
        try (InputStream inputStream = file.getInputStream()) {
            contentType = detect(inputStream);
        } catch (IOException e) {
            errors.rejectValue(fieldName, "error.file.unreadable");
            return;
        }

        if (!isAllowed(contentType)) {
            errors.rejectValue(fieldName, "error.file.type.unsupported");
            return;
        }

        if (StringUtils.hasText(requiredType) && !contentType.startsWith(requiredType)) {
            errors.rejectValue(fieldName, "error.only.type.file.allowed", new Object[]{requiredType},
                    "Only " + requiredType + " file is allowed");
        }
    }
}
