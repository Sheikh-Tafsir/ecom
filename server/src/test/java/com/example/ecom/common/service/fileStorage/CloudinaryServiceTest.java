package com.example.ecom.common.service.fileStorage;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.cloudinary.utils.ObjectUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private CloudinaryService cloudinaryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cloudinaryService, "folder", "ecom-uploads");
    }

    @Test
    @DisplayName("uploadFile uploads file and returns secure URL")
    void uploadFile_validFile_returnsSecureUrl() throws IOException {
        MockMultipartFile file = new MockMultipartFile("image", "product.jpg", "image/jpeg", "fake-content".getBytes());
        Map<String, Object> uploadResult = Map.of("secure_url", "https://res.cloudinary.com/demo/image/upload/ecom-uploads/abc123.jpg");

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(uploadResult);

        String result = cloudinaryService.uploadFile(file);

        assertEquals("https://res.cloudinary.com/demo/image/upload/ecom-uploads/abc123.jpg", result);
        verify(uploader).upload(any(byte[].class), anyMap());
    }

    @Test
    @DisplayName("uploadFile propagates IOException on failure")
    void uploadFile_ioError_throwsIOException() throws IOException {
        MockMultipartFile file = new MockMultipartFile("image", "product.jpg", "image/jpeg", "fake-content".getBytes());

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new IOException("Connection refused"));

        assertThrows(IOException.class, () -> cloudinaryService.uploadFile(file));
    }

    @Test
    @DisplayName("deleteFile deletes file from cloudinary using extracted public ID")
    void deleteFile_validUrl_deletesFile() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(anyString(), anyMap())).thenReturn(Map.of("result", "ok"));

        cloudinaryService.deleteFile("https://res.cloudinary.com/demo/image/upload/ecom-uploads/abc123.jpg");

        verify(uploader).destroy(eq("ecom-uploads/abc123"), anyMap());
    }

    @Test
    @DisplayName("deleteFile does nothing for null/blank URL")
    void deleteFile_nullUrl_doesNothing() {
        cloudinaryService.deleteFile(null);
        cloudinaryService.deleteFile("");
        cloudinaryService.deleteFile("  ");

        verify(cloudinary, never()).uploader();
    }

    @Test
    @DisplayName("deleteFile swallows exceptions gracefully")
    void deleteFile_exceptionThrown_doesNotPropagate() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(anyString(), anyMap())).thenThrow(new RuntimeException("API error"));

        assertDoesNotThrow(() -> cloudinaryService.deleteFile("https://example.com/folder/file.jpg"));
    }
}
