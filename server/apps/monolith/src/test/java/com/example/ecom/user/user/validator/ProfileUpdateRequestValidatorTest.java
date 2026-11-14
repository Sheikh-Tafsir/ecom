package com.example.ecom.user.user.validator;

import com.example.ecom.common.validator.CommonValidator;
import com.example.ecom.user.user.dto.UpdateProfileRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileUpdateRequestValidatorTest {

    @Mock
    private CommonValidator commonValidator;

    @InjectMocks
    private ProfileUpdateRequestValidator validator;

    private Errors errors;

    @BeforeEach
    void setUp() {
        errors = new BeanPropertyBindingResult(new Object(), "updateProfileRequest");
    }

    @Test
    @DisplayName("validate should delegate image validation to CommonValidator with image not required")
    void validate_withImage_delegatesToCommonValidator() {
        MultipartFile image = mock(MultipartFile.class);
        UpdateProfileRequest request = new UpdateProfileRequest("John Doe", image);

        validator.validate(request, errors);

        verify(commonValidator).validateImage(image, false, "image", errors);
    }

    @Test
    @DisplayName("validate should pass null image to CommonValidator for validation")
    void validate_withNullImage_delegatesToCommonValidator() {
        UpdateProfileRequest request = new UpdateProfileRequest("Jane Smith", null);

        validator.validate(request, errors);

        verify(commonValidator).validateImage(null, false, "image", errors);
    }

    @Test
    @DisplayName("validate should call validateImage exactly once")
    void validate_invoked_callsValidateImageOnce() {
        MultipartFile image = mock(MultipartFile.class);
        UpdateProfileRequest request = new UpdateProfileRequest("Test User", image);

        validator.validate(request, errors);

        verify(commonValidator, times(1)).validateImage(any(), eq(false), eq("image"), any());
    }

    @Test
    @DisplayName("validate should pass the false required flag to CommonValidator")
    void validate_always_passesRequiredFalse() {
        UpdateProfileRequest request = new UpdateProfileRequest("Alice Brown", null);

        validator.validate(request, errors);

        verify(commonValidator).validateImage(isNull(), eq(false), eq("image"), eq(errors));
    }
}
