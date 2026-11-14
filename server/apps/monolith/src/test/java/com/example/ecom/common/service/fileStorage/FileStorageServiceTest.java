package com.example.ecom.common.service.fileStorage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    private TestFileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new TestFileStorageService();
    }

    @Test
    @DisplayName("uploadFileAsync returns completed future with URL on success")
    void uploadFileAsync_success_returnsUrl() throws Exception {
        fileStorageService.setUploadUrl("https://cdn.example.com/image.jpg");
        MockMultipartFile file = new MockMultipartFile("image", "photo.jpg", "image/jpeg", "data".getBytes(StandardCharsets.UTF_8));

        CompletableFuture<String> future = fileStorageService.uploadFileAsync(file);

        assertEquals("https://cdn.example.com/image.jpg", future.get());
    }

    @Test
    @DisplayName("uploadFileAsync returns failed future on IOException")
    void uploadFileAsync_ioError_returnsFailedFuture() {
        fileStorageService.setThrowOnUpload(true);
        MockMultipartFile file = new MockMultipartFile("image", "photo.jpg", "image/jpeg", "data".getBytes(StandardCharsets.UTF_8));

        CompletableFuture<String> future = fileStorageService.uploadFileAsync(file);

        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    @DisplayName("deleteFileAsync returns completed future on success")
    void deleteFileAsync_success_completesNormally() throws Exception {
        CompletableFuture<Void> future = fileStorageService.deleteFileAsync("https://cdn.example.com/image.jpg");

        assertNull(future.get());
    }

    @Test
    @DisplayName("deleteFileAsync returns failed future when exception occurs")
    void deleteFileAsync_error_returnsFailedFuture() {
        fileStorageService.setThrowOnDelete(true);

        CompletableFuture<Void> future = fileStorageService.deleteFileAsync("https://cdn.example.com/image.jpg");

        assertTrue(future.isCompletedExceptionally());
    }

    /**
     * Concrete test implementation of the abstract FileStorageService
     */
    private static class TestFileStorageService extends FileStorageService {

        private String uploadUrl;
        private boolean throwOnUpload;
        private boolean throwOnDelete;

        void setUploadUrl(String uploadUrl) { this.uploadUrl = uploadUrl; }
        void setThrowOnUpload(boolean throwOnUpload) { this.throwOnUpload = throwOnUpload; }
        void setThrowOnDelete(boolean throwOnDelete) { this.throwOnDelete = throwOnDelete; }

        @Override
        public String uploadFile(MultipartFile file) throws IOException {
            if (throwOnUpload) throw new IOException("Upload failed");
            return uploadUrl;
        }

        @Override
        public void deleteFile(String fileUrl) {
            if (throwOnDelete) throw new RuntimeException("Delete failed");
        }
    }
}
