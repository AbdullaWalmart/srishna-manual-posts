package com.srishna.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;

/**
 * Schedules upload of the local SQLite DB to GCS after the current transaction commits.
 * Ensures the DB in GCS (gs://prod_srishna_web/data/srishna.db) is always up to date so
 * new posts and other changes persist across restarts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DbSyncHelper {

    private final DataSyncService dataSyncService;

    /**
     * Register a sync to upload the DB file to GCS after the current transaction commits.
     * Call this from any @Transactional method that modifies the database.
     * If no transaction is active, sync runs immediately.
     */
    public void syncToGcsAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doSync();
                }
            });
        } else {
            doSync();
        }
    }

    private void doSync() {
        try {
            dataSyncService.uploadDbToGcs();
        } catch (IOException e) {
            log.error("Failed to sync DB to GCS: {}", e.getMessage());
        }
    }
}
