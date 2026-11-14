package com.example.ecom.product.product.validator;

import com.example.ecom.common.validator.CommonValidator;
import com.example.ecom.product.product.dto.CreateProductRequest;
import com.example.ecom.product.product.dto.UpdateProductRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductValidatorTest {

    @Mock
    private CommonValidator commonValidator;

    @InjectMocks
    private ProductValidator productValidator;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(productValidator, "maxImageCount", 5);
    }

    @Test
    @DisplayName("validateCreate should pass when image count is within limit")
    void validateCreate_withinLimit_noErrors() {
        MultipartFile file1 = mock(MultipartFile.class);
        MultipartFile file2 = mock(MultipartFile.class);
        Set<MultipartFile> images = Set.of(file1, file2);

        CreateProductRequest request = new CreateProductRequest();
        request.setImages(images);
        Errors errors = new BeanPropertyBindingResult(request, "request");

        productValidator.validateCreate(request, errors);

        assertFalse(errors.hasErrors());
        verify(commonValidator, times(2)).validateImage(any(MultipartFile.class), eq(false), eq("images"), eq(errors));
    }

    @Test
    @DisplayName("validateCreate should reject when image count exceeds max limit")
    void validateCreate_exceedsMaxImageCount_rejectsWithError() {
        Set<MultipartFile> images = new HashSet<>();
        for (int i = 0; i < 6; i++) {
            images.add(mock(MultipartFile.class));
        }

        CreateProductRequest request = new CreateProductRequest();
        request.setImages(images);
        Errors errors = new BeanPropertyBindingResult(request, "request");

        productValidator.validateCreate(request, errors);

        assertTrue(errors.hasErrors());
        assertNotNull(errors.getFieldError("images"));
        verify(commonValidator, never()).validateImage(any(), anyBoolean(), anyString(), any());
    }

    @Test
    @DisplayName("validateCreate should validate each image file when within limit")
    void validateCreate_threeImages_validatesEachFile() {
        MultipartFile file1 = mock(MultipartFile.class);
        MultipartFile file2 = mock(MultipartFile.class);
        MultipartFile file3 = mock(MultipartFile.class);
        Set<MultipartFile> images = Set.of(file1, file2, file3);

        CreateProductRequest request = new CreateProductRequest();
        request.setImages(images);
        Errors errors = new BeanPropertyBindingResult(request, "request");

        productValidator.validateCreate(request, errors);

        assertFalse(errors.hasErrors());
        verify(commonValidator, times(3)).validateImage(any(MultipartFile.class), eq(false), eq("images"), eq(errors));
    }

    @Test
    @DisplayName("validateCreate should pass with exactly max images")
    void validateCreate_exactlyMaxImages_noErrors() {
        Set<MultipartFile> images = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            images.add(mock(MultipartFile.class));
        }

        CreateProductRequest request = new CreateProductRequest();
        request.setImages(images);
        Errors errors = new BeanPropertyBindingResult(request, "request");

        productValidator.validateCreate(request, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("validateUpdate should pass when combined old and new image count is within limit")
    void validateUpdate_combinedCountWithinLimit_noErrors() {
        UUID keptId1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID keptId2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
        MultipartFile newFile = mock(MultipartFile.class);

        UpdateProductRequest request = new UpdateProductRequest();
        request.setKeptImageIds(Set.of(keptId1, keptId2));
        request.setImages(Set.of(newFile));
        Errors errors = new BeanPropertyBindingResult(request, "request");

        productValidator.validateUpdate(request, errors);

        assertFalse(errors.hasErrors());
        verify(commonValidator).validateImage(eq(newFile), eq(false), eq("images"), eq(errors));
    }

    @Test
    @DisplayName("validateUpdate should reject when combined old and new image count exceeds limit")
    void validateUpdate_combinedCountExceedsLimit_rejectsWithError() {
        Set<UUID> keptImageIds = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            keptImageIds.add(UUID.randomUUID());
        }

        Set<MultipartFile> newImages = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            newImages.add(mock(MultipartFile.class));
        }

        UpdateProductRequest request = new UpdateProductRequest();
        request.setKeptImageIds(keptImageIds);
        request.setImages(newImages);
        Errors errors = new BeanPropertyBindingResult(request, "request");

        productValidator.validateUpdate(request, errors);

        assertTrue(errors.hasErrors());
        verify(commonValidator, never()).validateImage(any(), anyBoolean(), anyString(), any());
    }

    @Test
    @DisplayName("validateUpdate should handle null kept image IDs as zero old images")
    void validateUpdate_nullKeptImageIds_treatsAsZero() {
        MultipartFile newFile = mock(MultipartFile.class);

        UpdateProductRequest request = new UpdateProductRequest();
        request.setKeptImageIds(null);
        request.setImages(Set.of(newFile));
        Errors errors = new BeanPropertyBindingResult(request, "request");

        productValidator.validateUpdate(request, errors);

        assertFalse(errors.hasErrors());
        verify(commonValidator).validateImage(eq(newFile), eq(false), eq("images"), eq(errors));
    }

    @Test
    @DisplayName("validateUpdate should handle null new images as zero new images")
    void validateUpdate_nullNewImages_treatsAsZero() {
        UUID keptId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        UpdateProductRequest request = new UpdateProductRequest();
        request.setKeptImageIds(Set.of(keptId));
        request.setImages(null);
        Errors errors = new BeanPropertyBindingResult(request, "request");

        productValidator.validateUpdate(request, errors);

        assertFalse(errors.hasErrors());
        verify(commonValidator, never()).validateImage(any(), anyBoolean(), anyString(), any());
    }

    @Test
    @DisplayName("validateUpdate should handle both null kept and null new images")
    void validateUpdate_bothNull_noErrors() {
        UpdateProductRequest request = new UpdateProductRequest();
        request.setKeptImageIds(null);
        request.setImages(null);
        Errors errors = new BeanPropertyBindingResult(request, "request");

        productValidator.validateUpdate(request, errors);

        assertFalse(errors.hasErrors());
    }
}
