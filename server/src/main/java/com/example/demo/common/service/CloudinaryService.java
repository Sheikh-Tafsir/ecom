package com.example.demo.common.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public String uploadFile(MultipartFile image) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(image.getBytes(),
                ObjectUtils.asMap("folder", "chat-images"));
        return uploadResult.get("secure_url").toString();
    }
}
