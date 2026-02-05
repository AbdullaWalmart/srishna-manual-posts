package com.srishna.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Runs before the DataSource is created. Always downloads the SQLite DB from the GCS bucket
 * (gs://bucket/data/srishna.db) to the local path so both local and Cloud Run use the same DB.
 * Source: https://storage.googleapis.com/prod_srishna_web/data/srishna.db
 */
public class GcpDbRestoreInitializer implements org.springframework.context.ApplicationContextInitializer<org.springframework.context.ConfigurableApplicationContext> {

    @Override
    public void initialize(org.springframework.context.ConfigurableApplicationContext context) {
        Environment env = context.getEnvironment();
        String bucketName = env.getProperty("gcp.bucket-name");
        if (!StringUtils.hasText(bucketName)) {
            bucketName = System.getenv("GCP_BUCKET");
        }
        if (!StringUtils.hasText(bucketName)) {
            return;
        }
        String dbObjectName = env.getProperty("gcp.db-object-name", "data/srishna.db");
        String localPath = env.getProperty("SQLITE_PATH");
        if (localPath == null || localPath.isEmpty()) {
            localPath = System.getProperty("java.io.tmpdir") + "/srishna.db";
        }
        Path path = Paths.get(localPath).toAbsolutePath().normalize();
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Storage storage = createStorage(env);
            BlobId blobId = BlobId.of(bucketName, dbObjectName);
            com.google.cloud.storage.Blob blob = storage.get(blobId);
            if (blob != null && blob.exists()) {
                byte[] bytes = blob.getContent();
                if (bytes != null && bytes.length > 0) {
                    Files.write(path, bytes);
                }
            }
            // If no blob in GCS, SQLite will create DB on first connect; first write will upload to GCS
        } catch (Exception e) {
            System.err.println("[GcpDb] Load failed (will use existing local file if present): " + e.getMessage());
        }
    }

    private static Storage createStorage(Environment env) throws IOException {
        String projectId = env.getProperty("gcp.project-id");
        if (!StringUtils.hasText(projectId)) projectId = System.getenv("GCP_PROJECT_ID");
        StorageOptions.Builder builder = StorageOptions.newBuilder();
        if (StringUtils.hasText(projectId)) builder.setProjectId(projectId);
        GoogleCredentials credentials = null;
        String credPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (!StringUtils.hasText(credPath)) credPath = env.getProperty("gcp.credentials-path");
        if (StringUtils.hasText(credPath)) {
            if (credPath.startsWith("classpath:")) {
                String resource = credPath.substring("classpath:".length());
                try (InputStream is = GcpDbRestoreInitializer.class.getResourceAsStream("/" + resource)) {
                    if (is != null) credentials = GoogleCredentials.fromStream(is);
                }
            } else {
                try (InputStream is = new FileInputStream(credPath)) {
                    credentials = GoogleCredentials.fromStream(is);
                }
            }
        }
        if (credentials == null) {
            credentials = GoogleCredentials.getApplicationDefault();
        }
        builder.setCredentials(credentials);
        return builder.build().getService();
    }
}
