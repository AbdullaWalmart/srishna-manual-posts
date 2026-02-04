package com.srishna.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Syncs the SQLite DB file with the prod_srishna_web bucket so data survives deployment.
 * Backup uploads the current DB to GCS; restore is done at startup via {@link com.srishna.config.GcpDbRestoreInitializer}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataSyncService {

    private final Storage storage;

    @Value("${gcp.bucket-name}")
    private String bucketName;

    @Value("${gcp.db-object-name:data/srishna.db}")
    private String dbObjectName;

    @Value("${app.db-path:./data/srishna.db}")
    private String dbPath;

    /**
     * Uploads the current SQLite DB file to the GCS bucket (e.g. gs://prod_srishna_web/data/srishna.db).
     * Call this before deployment or periodically so the bucket has the latest data.
     */
    public void uploadDbToGcs() throws IOException {
        Path path = Paths.get(dbPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            throw new IOException("DB file not found: " + path);
        }
        byte[] bytes = Files.readAllBytes(path);
        BlobId blobId = BlobId.of(bucketName, dbObjectName);
        storage.create(com.google.cloud.storage.BlobInfo.newBuilder(blobId)
                .setContentType("application/x-sqlite3")
                .build(), bytes);
        log.info("Uploaded DB to gs://{}/{}", bucketName, dbObjectName);
    }

    /**
     * Downloads the SQLite DB from the GCS bucket to the local path (e.g. /tmp/srishna.db).
     * Use this to revert to the bucket's data and avoid losing existing data from the bucket.
     * Restart the application after calling this so it uses the reverted DB.
     */
    public void downloadDbFromGcs() throws IOException {
        BlobId blobId = BlobId.of(bucketName, dbObjectName);
        Blob blob = storage.get(blobId);
        if (blob == null || !blob.exists()) {
            throw new IOException("DB object not found in GCS: gs://" + bucketName + "/" + dbObjectName);
        }
        byte[] bytes = blob.getContent();
        Path path = Paths.get(dbPath).toAbsolutePath().normalize();
        Files.createDirectories(path.getParent());
        Files.write(path, bytes);
        log.info("Reverted DB from gs://{}/{} to {}", bucketName, dbObjectName, path);
    }
}
