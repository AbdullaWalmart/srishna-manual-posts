package com.srishna.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final Storage storage;

    @Value("${gcp.bucket-name}")
    private String bucketName;

    private static final String IMAGES_PREFIX = "images/";
    private static final String TEXTS_PREFIX = "texts/";

    /** Upload image to bucket/images/{uuid}.{ext} */
    public String uploadImage(MultipartFile file) throws IOException {
        String ext = extension(file.getOriginalFilename(), "jpg");
        String name = IMAGES_PREFIX + UUID.randomUUID() + "." + ext;
        BlobId blobId = BlobId.of(bucketName, name);
        BlobInfo info = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType() != null ? file.getContentType() : "image/jpeg")
                .build();
        storage.create(info, file.getBytes());
        return name;
    }

    /** Upload text to bucket/texts/{uuid}.txt */
    public String uploadText(String content) throws IOException {
        String name = TEXTS_PREFIX + UUID.randomUUID() + ".txt";
        BlobId blobId = BlobId.of(bucketName, name);
        BlobInfo info = BlobInfo.newBuilder(blobId)
                .setContentType("text/plain; charset=utf-8")
                .build();
        storage.create(info, content.getBytes(StandardCharsets.UTF_8));
        return name;
    }

    /** Returns a signed URL valid for 1 hour so the frontend can load the image from a private bucket. */
    public String getSignedUrl(String objectPath) {
        if (objectPath == null || objectPath.isEmpty()) return null;
        try {
            BlobInfo info = BlobInfo.newBuilder(BlobId.of(bucketName, objectPath)).build();
            URL signed = storage.signUrl(info, 1, TimeUnit.HOURS, Storage.SignUrlOption.httpMethod(HttpMethod.GET));
            return signed.toString();
        } catch (Exception e) {
            log.warn("Failed to sign URL for {}: {}", objectPath, e.getMessage());
            return null;
        }
    }

    /** Returns URL for the object (signed URL for private buckets). */
    public String getPublicUrl(String objectPath) {
        return getSignedUrl(objectPath);
    }

    private static String extension(String filename, String fallback) {
        if (filename == null || !filename.contains(".")) return fallback;
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
