package com.example.demo.product.validator;

import com.example.demo.common.validator.CommonValidator;
import com.example.demo.product.dto.CreateProductRequest;
import com.example.demo.product.dto.UpdateProductRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProductValidator {

    public static final int MAX_IMAGE_COUNT = 5;

    private final CommonValidator commonValidator;

    public void validateCreate(CreateProductRequest request, Errors errors) {
        validate(request.getImages(), errors);
    }

    public void validateUpdate(UpdateProductRequest request, Errors errors) {
        validate(request.getImages(), errors);
    }

    private void validate(Set<MultipartFile> request, Errors errors) {
        if (request.size() > MAX_IMAGE_COUNT) {
            errors.rejectValue("images", "error.file.quantity", new Object[]{MAX_IMAGE_COUNT},
                    "Cannot upload more than " + MAX_IMAGE_COUNT + " files");
            return;
        }

        for (MultipartFile image : request) {
            commonValidator.validateImage(image, false, "images", errors);
        }
    }
}
