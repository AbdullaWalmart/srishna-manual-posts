package com.srishna.controller;

import com.srishna.service.DataSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Admin endpoints for DB sync with GCS bucket.
 * POST /api/admin/backup-db – upload current DB to bucket.
 * POST /api/admin/revert-db – download DB from bucket to local (revert); restart app to use it.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DataSyncService dataSyncService;

    /**
     * Upload the current database file to GCS (gs://bucket/data/srishna.db)
     * so data persists across deployments. Call before deploy or on a schedule.
     */
    @PostMapping("/backup-db")
    public ResponseEntity<String> backupDb() {
        try {
            dataSyncService.uploadDbToGcs();
            return ResponseEntity.ok("DB backed up to GCS successfully.");
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Backup failed: " + e.getMessage());
        }
    }

    /**
     * Revert: download the DB from the GCP bucket to the local path (e.g. /tmp/srishna.db).
     * Use this to restore data from the bucket so you do not lose existing data.
     * Restart the application after calling this so it uses the reverted database.
     */
    @PostMapping("/revert-db")
    public ResponseEntity<String> revertDb() {
        try {
            dataSyncService.downloadDbFromGcs();
            return ResponseEntity.ok(
                    "DB reverted from GCS to local successfully. Restart the application to use the reverted data.");
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Revert failed: " + e.getMessage());
        }
    }
}
