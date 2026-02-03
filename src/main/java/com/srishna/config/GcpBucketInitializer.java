package com.srishna.config;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GcpBucketInitializer implements ApplicationRunner {

    private final Storage storage;

    @Value("${gcp.bucket-name}")
    private String bucketName;

    @Override
    public void run(ApplicationArguments args) {
        Bucket bucket = storage.get(bucketName);
        if (bucket == null) {
            try {
                bucket = storage.create(BucketInfo.of(bucketName));
                log.info("Created GCS bucket: {}", bucketName);
            } catch (Exception e) {
                log.warn("Could not create bucket {}: {}", bucketName, e.getMessage());
            }
        } else {
            log.debug("GCS bucket already exists: {}", bucketName);
        }
    }
}
