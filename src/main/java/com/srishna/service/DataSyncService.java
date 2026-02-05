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
 * Syncs the SQLite DB file with the GCS bucket (gs://prod_srishna_web/data/srishna.db).
 * On startup the DB is loaded from GCS via {@link com.srishna.config.GcpDbRestoreInitializer}.
 * Backup uploads the current DB to GCS.
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

    @Value("${app.db-path}")
    private String dbPath;

    /**
     * Uploads the current SQLite DB file to GCS (gs://bucket/data/srishna.db).
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
     * Downloads the DB from GCS to the local path. Restart the application after calling to use it.
     */
    public void downloadDbFromGcs() throws IOException {
        BlobId blobId = BlobId.of(bucketName, dbObjectName);
        Blob blob = storage.get(blobId);
        if (blob == null || !blob.exists()) {
            throw new IOException("DB not found in GCS: gs://" + bucketName + "/" + dbObjectName);
        }
        byte[] bytes = blob.getContent();
        Path path = Paths.get(dbPath).toAbsolutePath().normalize();
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.write(path, bytes);
        log.info("Downloaded DB from gs://{}/{} to {}", bucketName, dbObjectName, path);
    }
}
