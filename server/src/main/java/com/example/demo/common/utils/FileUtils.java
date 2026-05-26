package com.example.demo.common.utils;

import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public final class FileUtils {

    private FileUtils() {}

    private static final Tika TIKA = new Tika();

    public static final int MAX_FILE_SIZE = 5 * 1024 * 1024;

    public static final Set<String> ALLOWED_TYPES = Set.of(
            //images
            "image/jpeg", "image/png", "image/gif"
    );

    public static boolean fileExists(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    public static boolean fileTooLarge(MultipartFile file) {
        return file.getSize() > MAX_FILE_SIZE;
    }

    public static String detect(InputStream inputStream) throws IOException {
        return TIKA.detect(inputStream);
    }

    public static boolean isAllowed(String contentType) {
        return ALLOWED_TYPES.contains(contentType);
    }
}
