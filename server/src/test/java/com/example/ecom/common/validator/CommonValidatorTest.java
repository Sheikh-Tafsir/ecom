package com.example.ecom.common.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CommonValidatorTest {

    @InjectMocks
    private CommonValidator commonValidator;

    private Errors errors;

    @BeforeEach
    void setUp() {
        errors = new BeanPropertyBindingResult(new TestTarget(), "testTarget");
    }

    // --- validateString ---

    @Test
    @DisplayName("validateString rejects empty value when required")
    void validateString_requiredAndEmpty_addsError() {
        commonValidator.validateString("", true, 2, 50, "name", errors);
        assertTrue(errors.hasFieldErrors("name"));
    }

    @Test
    @DisplayName("validateString rejects null value when required")
    void validateString_requiredAndNull_addsError() {
        commonValidator.validateString(null, true, 2, 50, "name", errors);
        assertTrue(errors.hasFieldErrors("name"));
    }

    @Test
    @DisplayName("validateString rejects blank whitespace value when required")
    void validateString_requiredAndBlank_addsError() {
        commonValidator.validateString("   ", true, 2, 50, "name", errors);
        assertTrue(errors.hasFieldErrors("name"));
    }

    @Test
    @DisplayName("validateString accepts valid value within range")
    void validateString_validValue_noError() {
        commonValidator.validateString("John Doe", true, 2, 50, "name", errors);
        assertFalse(errors.hasFieldErrors("name"));
    }

    @Test
    @DisplayName("validateString rejects value shorter than min length")
    void validateString_tooShort_addsError() {
        commonValidator.validateString("A", true, 2, 50, "name", errors);
        assertTrue(errors.hasFieldErrors("name"));
    }

    @Test
    @DisplayName("validateString rejects value longer than max length")
    void validateString_tooLong_addsError() {
        String longValue = "A".repeat(51);
        commonValidator.validateString(longValue, true, 2, 50, "name", errors);
        assertTrue(errors.hasFieldErrors("name"));
    }

    @Test
    @DisplayName("validateString accepts value at exact min boundary")
    void validateString_exactMinLength_noError() {
        commonValidator.validateString("AB", true, 2, 50, "name", errors);
        assertFalse(errors.hasFieldErrors("name"));
    }

    @Test
    @DisplayName("validateString accepts value at exact max boundary")
    void validateString_exactMaxLength_noError() {
        String value = "A".repeat(50);
        commonValidator.validateString(value, true, 2, 50, "name", errors);
        assertFalse(errors.hasFieldErrors("name"));
    }

    // --- validateImage ---

    @Test
    @DisplayName("validateImage rejects missing file when required")
    void validateImage_requiredAndNull_addsError() {
        commonValidator.validateImage(null, true, "image", errors);
        assertTrue(errors.hasFieldErrors("image"));
    }

    @Test
    @DisplayName("validateImage accepts missing file when not required")
    void validateImage_notRequiredAndNull_noError() {
        commonValidator.validateImage(null, false, "image", errors);
        assertFalse(errors.hasFieldErrors("image"));
    }

    @Test
    @DisplayName("validateImage rejects empty file when required")
    void validateImage_requiredAndEmpty_addsError() {
        MockMultipartFile emptyFile = new MockMultipartFile("image", "", "image/png", new byte[0]);
        commonValidator.validateImage(emptyFile, true, "image", errors);
        assertTrue(errors.hasFieldErrors("image"));
    }

    @Test
    @DisplayName("validateImage accepts valid JPEG image")
    void validateImage_validJpeg_noError() {
        // JPEG magic bytes: FF D8 FF
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00};
        MockMultipartFile jpegFile = new MockMultipartFile("image", "photo.jpg", "image/jpeg", jpegBytes);

        commonValidator.validateImage(jpegFile, true, "image", errors);
        assertFalse(errors.hasFieldErrors("image"));
    }

    @Test
    @DisplayName("validateImage rejects file exceeding max size")
    void validateImage_tooLargeFile_addsError() {
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB, exceeds default 5MB
        MockMultipartFile largeFile = new MockMultipartFile("image", "large.jpg", "image/jpeg", largeContent);

        commonValidator.validateImage(largeFile, true, "image", errors);
        assertTrue(errors.hasFieldErrors("image"));
    }

    // Inner class used as target for BeanPropertyBindingResult
    @SuppressWarnings("unused")
    private static class TestTarget {
        private String name;
        private Object image;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Object getImage() { return image; }
        public void setImage(Object image) { this.image = image; }
    }
}
