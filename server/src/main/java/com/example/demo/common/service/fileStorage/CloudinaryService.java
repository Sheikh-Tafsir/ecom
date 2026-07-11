package com.example.demo.common.service.fileStorage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
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

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return;

        try {
            String publicIdWithExtension = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            String publicId = publicIdWithExtension.substring(0, publicIdWithExtension.lastIndexOf("."));
            String fullPublicId = folder + "/" + publicId;

            cloudinary.uploader().destroy(fullPublicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            log.error("Could not delete image from cloudinary", e);
        }
    }
}
