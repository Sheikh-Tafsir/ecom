package com.example.ecom.product.product.validator;

import com.example.ecom.common.validator.CommonValidator;
import com.example.ecom.product.product.dto.CreateProductRequest;
import com.example.ecom.product.product.dto.UpdateProductRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

import static com.example.ecom.common.utils.Utils.isEmpty;

@Component
@RefreshScope
@RequiredArgsConstructor
public class ProductValidator {

    @Value("${app.product.max-image-count:5}")
    private int maxImageCount;

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
        if (imageCount > maxImageCount) {
            errors.rejectValue("images", "error.file.quantity", new Object[]{maxImageCount},
                    "Cannot upload more than " + maxImageCount + " files");
            return;
        }

        if (!isEmpty(imageFiles)) {
            for (MultipartFile image : imageFiles) {
                commonValidator.validateImage(image, false, "images", errors);
            }
        }
    }
}
