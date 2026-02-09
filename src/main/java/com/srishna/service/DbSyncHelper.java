package com.srishna.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;

/**
 * Schedules upload of the local SQLite DB to GCS after the current transaction commits.
 * Upload runs asynchronously so API responses return quickly; sync continues in the background.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DbSyncHelper {

    private final DataSyncService dataSyncService;

    /**
     * Register a sync to upload the DB file to GCS after the current transaction commits.
     * Upload runs in a background thread so the request returns without waiting for GCS.
     */
    public void syncToGcsAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    uploadDbToGcsAsync();
                }
            });
        } else {
            uploadDbToGcsAsync();
        }
    }

    @Async("dbSyncExecutor")
    public void uploadDbToGcsAsync() {
        try {
            dataSyncService.uploadDbToGcs();
        } catch (IOException e) {
            log.error("Failed to sync DB to GCS: {}", e.getMessage());
        }
    }
}
