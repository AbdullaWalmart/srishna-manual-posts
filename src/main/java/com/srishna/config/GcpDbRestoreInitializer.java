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
 * Runs before the DataSource is created. When GCP_DB_RESTORE=true, downloads
 * the SQLite DB from the prod_srishna_web bucket to the local path so deployment
 * uses the same data.
 */
public class GcpDbRestoreInitializer implements org.springframework.context.ApplicationContextInitializer<org.springframework.context.ConfigurableApplicationContext> {

    private static final String RESTORE_ENV = "GCP_DB_RESTORE";

    @Override
    public void initialize(org.springframework.context.ConfigurableApplicationContext context) {
        Environment env = context.getEnvironment();
        if (!"true".equalsIgnoreCase(env.getProperty(RESTORE_ENV))) {
            return;
        }
        String bucketName = env.getProperty("gcp.bucket-name");
        String dbObjectName = env.getProperty("gcp.db-object-name", "data/srishna.db");
        String localPath = env.getProperty("SQLITE_PATH", env.getProperty("app.db-path", "./data/srishna.db"));
        if (!StringUtils.hasText(bucketName)) {
            bucketName = System.getenv("GCP_BUCKET");
            if (!StringUtils.hasText(bucketName)) return;
        }
        try {
            Storage storage = createStorage(env);
            byte[] bytes = storage.get(BlobId.of(bucketName, dbObjectName)).getContent();
            if (bytes == null || bytes.length == 0) return;
            Path path = Paths.get(localPath).toAbsolutePath().normalize();
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
            System.out.println("[GcpDbRestore] Restored DB from gs://" + bucketName + "/" + dbObjectName + " to " + path);
        } catch (Exception e) {
            System.err.println("[GcpDbRestore] Restore failed: " + e.getMessage());
            // Do not fail startup if blob does not exist (first deploy)
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
