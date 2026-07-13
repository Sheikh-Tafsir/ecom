package com.example.demo.common.service.fileStorage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class FileStorageService {

    public abstract String uploadFile(MultipartFile file) throws IOException;

    @Async("taskExecutor")
    public CompletableFuture<String> uploadFileAsync(MultipartFile file) {
        try {
            return CompletableFuture.completedFuture(uploadFile(file));
        } catch (IOException e) {
            log.error("Image upload fail", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public abstract void deleteFile(String fileUrl);

    @Async("taskExecutor")
    public CompletableFuture<Void> deleteFileAsync(String fileUrl) {
        try {
            deleteFile(fileUrl);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Image delete fail", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
