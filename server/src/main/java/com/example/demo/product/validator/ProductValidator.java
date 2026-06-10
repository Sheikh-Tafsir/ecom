package com.example.demo.product.validator;

import com.example.demo.common.validator.CommonValidator;
import com.example.demo.product.dto.CreateProductRequest;
import com.example.demo.product.dto.UpdateProductRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

import static com.example.demo.common.utils.Utils.isEmpty;

@Component
@RequiredArgsConstructor
public class ProductValidator {

    public static final int MAX_IMAGE_COUNT = 5;

    private final CommonValidator commonValidator;

    public void validateCreate(CreateProductRequest request, Errors errors) {
        validateImages(request.getImages().size(), request.getImages(), errors);
    }

    public void validateUpdate(UpdateProductRequest request, Errors errors) {
        int oldImageCount = isEmpty(request.getKeptImageIds()) ? 0 : request.getKeptImageIds().size();
        int newImageCount = isEmpty(request.getImages()) ? 0 : request.getImages().size();

        validateImages(oldImageCount + newImageCount, request.getImages(), errors);
    }

    private void validateImages(int imageCount, Set<MultipartFile> imageFiles, Errors errors) {
        if (imageCount > MAX_IMAGE_COUNT) {
            errors.rejectValue("images", "error.file.quantity", new Object[]{MAX_IMAGE_COUNT},
                    "Cannot upload more than " + MAX_IMAGE_COUNT + " files");
            return;
        }

        if (!isEmpty(imageFiles)) {
            for (MultipartFile image : imageFiles) {
                commonValidator.validateImage(image, false, "images", errors);
            }
        }
    }
}
