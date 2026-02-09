package com.srishna.service;

import com.github.benmanes.caffeine.cache.Caffeine;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private static final String IMAGES_PREFIX = "images/";
    private static final String TEXTS_PREFIX = "texts/";
    private static final String GCS_PUBLIC_BASE = "https://storage.googleapis.com/";

    private final Storage storage;

    @Value("${gcp.bucket-name}")
    private String bucketName;

    @Value("${gcp.public-urls:false}")
    private boolean publicUrls;

    /** Cache signed URLs by object path so browser can cache image response; long validity = load without re-signing. */
    private static final int SIGNED_URL_VALIDITY_HOURS = 24;
    private static final int SIGNED_URL_CACHE_MINUTES = 23 * 60; // just under validity

    private final com.github.benmanes.caffeine.cache.Cache<String, String> signedUrlCache = Caffeine.newBuilder()
            .maximumSize(2000)
            .expireAfterWrite(SIGNED_URL_CACHE_MINUTES, TimeUnit.MINUTES)
            .build();

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

    /** Returns a signed URL valid 24h; cached so same URL is reused and browser can cache the image (loads live, no buffer). */
    public String getSignedUrl(String objectPath) {
        if (objectPath == null || objectPath.isEmpty()) return null;
        String cached = signedUrlCache.getIfPresent(objectPath);
        if (cached != null) return cached;
        try {
            BlobInfo info = BlobInfo.newBuilder(BlobId.of(bucketName, objectPath)).build();
            URL signed = storage.signUrl(info, SIGNED_URL_VALIDITY_HOURS, TimeUnit.HOURS, Storage.SignUrlOption.httpMethod(HttpMethod.GET));
            String url = signed != null ? signed.toString() : null;
            if (url != null) signedUrlCache.put(objectPath, url);
            return url;
        } catch (Exception e) {
            log.warn("Failed to sign URL for {}: {}", objectPath, e.getMessage());
            return null;
        }
    }

    /** Returns URL for the object: public GCS URL if gcp.public-urls=true, else cached signed URL. */
    public String getPublicUrl(String objectPath) {
        if (objectPath == null || objectPath.isEmpty()) return null;
        if (publicUrls) {
            // Encode path but keep slashes so URL is https://storage.googleapis.com/bucket/images/name.jpg
            String encoded = URLEncoder.encode(objectPath, StandardCharsets.UTF_8).replace("+", "%20").replace("%2F", "/");
            return GCS_PUBLIC_BASE + bucketName + "/" + encoded;
        }
        return getSignedUrl(objectPath);
    }

    private static String extension(String filename, String fallback) {
        if (filename == null || !filename.contains(".")) return fallback;
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
