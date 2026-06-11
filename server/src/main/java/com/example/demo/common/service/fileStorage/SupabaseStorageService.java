package com.example.demo.common.service.fileStorage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class SupabaseStorageService implements FileStorageService {

    private static final String STORAGE_PATH = "/storage/v1/object/";

    private static final String DEFAULT_EXTENSION = ".jpg";

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.api-key}")
    private String apiKey;

    @Value("${supabase.bucket}")
    private String bucket;

//    private final RestTemplate restTemplate;

    private final WebClient webClient;

    @Override
    public String uploadFile(MultipartFile file) throws IOException {

        String originalFilename = file.getOriginalFilename();

        String extension = DEFAULT_EXTENSION;
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String filePath = UUID.randomUUID() + extension;

//        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + filePath;
        String uploadUrl = getUploadUrl(filePath);

//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(apiKey);
//        headers.set("apikey", apiKey);
//
//        headers.setContentType(
//                MediaType.parseMediaType(
//                        Objects.requireNonNullElse(
//                                file.getContentType(),
//                                "application/octet-stream"
//                        )
//                )
//        );
//
//        HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);
//
//        restTemplate.exchange(
//                uploadUrl,
//                HttpMethod.PUT,
//                entity,
//                String.class
//        );

        webClient.put()
                .uri(uploadUrl)
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.parseMediaType(
                        Objects.requireNonNullElse(
                                file.getContentType(),
                                "application/octet-stream"
                        )
                ))
                .bodyValue(file.getBytes())
                .retrieve()
                .toBodilessEntity()
                .block();

//        return supabaseUrl +
//                "/storage/v1/object/public/" +
//                bucket +
//                "/" +
//                filePath;

        return getViewUrl(filePath);
    }

    private String getUploadUrl(String filePath) {
        return supabaseUrl + STORAGE_PATH + bucket + "/" + filePath;
    }

    private String getViewUrl(String filePath) {
        return supabaseUrl + STORAGE_PATH + "public/" + bucket + "/" + filePath;
    }
}