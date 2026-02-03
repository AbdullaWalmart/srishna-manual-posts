package com.srishna.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class GcpConfig {

    @Value("${gcp.project-id}")
    private String projectId;

    @Value("${gcp.credentials-path:}")
    private String credentialsPath;

    @Bean
    public Storage storage(ResourceLoader resourceLoader) throws IOException {
        StorageOptions.Builder builder = StorageOptions.newBuilder().setProjectId(projectId);
        String credPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (StringUtils.hasText(credPath)) {
            try (InputStream is = new java.io.FileInputStream(credPath)) {
                builder.setCredentials(GoogleCredentials.fromStream(is));
            }
        } else if (StringUtils.hasText(credentialsPath)) {
            if (credentialsPath.startsWith("classpath:")) {
                try (InputStream is = resourceLoader.getResource(credentialsPath).getInputStream()) {
                    builder.setCredentials(GoogleCredentials.fromStream(is));
                }
            } else {
                try (InputStream is = new java.io.FileInputStream(credentialsPath)) {
                    builder.setCredentials(GoogleCredentials.fromStream(is));
                }
            }
        } else {
            builder.setCredentials(GoogleCredentials.getApplicationDefault());
        }
        return builder.build().getService();
    }
}
