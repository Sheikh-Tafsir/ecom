package com.example.demo.product.validator;

import com.example.demo.common.validator.CommonValidator;
import com.example.demo.product.dto.ProductRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class ProductValidator implements Validator {

    public static final int MAX_IMAGE_COUNT = 5;

    private final CommonValidator commonValidator;

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz.equals(ProductRequest.class);
    }

    @Override
    public void validate(@NonNull Object target, @NonNull Errors errors) {
        ProductRequest request = (ProductRequest) target;

        if (request.images() == null || request.images().isEmpty()) {
            return;
        }

        if (request.images().size() > MAX_IMAGE_COUNT) {
            errors.rejectValue("images", "error.file.quantity", new Object[]{MAX_IMAGE_COUNT},
                    "Cannot upload more than " + MAX_IMAGE_COUNT + " files");
            return;
        }

        for (MultipartFile image : request.images()) {
            commonValidator.validateImage(image, false, "images", errors);
        }
    }
}
