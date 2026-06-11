package com.example.demo.common.service.fileStorage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService implements FileStorageService {

    @Value("${cloudinary.folder}")
    private String folder;

    private final Cloudinary cloudinary;

    public String uploadFile(MultipartFile image) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(image.getBytes(),
                ObjectUtils.asMap("folder", folder));

        return uploadResult.get("secure_url").toString();
    }
}
