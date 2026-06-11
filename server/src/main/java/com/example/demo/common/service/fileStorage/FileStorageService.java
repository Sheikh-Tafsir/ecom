package com.example.demo.common.service.fileStorage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorageService {

    String uploadFile(MultipartFile file) throws IOException;
}
