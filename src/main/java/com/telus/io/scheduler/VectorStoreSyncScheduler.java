package com.telus.io.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.telus.io.dto.SyncResult;
import com.telus.io.service.ResumeStorageService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enhanced scheduler for vector store synchronization.
 * This is now the primary way resumes enter the vector store system.
 */
@Component
public class VectorStoreSyncScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(VectorStoreSyncScheduler.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final ResumeStorageService storageService;
    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);
    private LocalDateTime lastSyncTime;
    private SyncResult lastSyncResult;
    
    @Autowired
    @Qualifier("resumeProcessingExecutor")
    private Executor resumeProcessingExecutor;
    
    public VectorStoreSyncScheduler(ResumeStorageService storageService) {
        this.storageService = storageService;
    }
    
    /**
     * Synchronize the vector store with the database daily at 2 AM.
     * This ensures that:
     * 1. Every resume in the database has exactly one entry in the vector store
     * 2. There are no orphaned entries in the vector store
     * 3. There are no duplicate entries in the vector store
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM every day
    public void scheduledSync() {
        logger.info("Starting scheduled vector store synchronization");
        
        try {
            SyncResult result = synchronizeVectorStore();
            logger.info("Scheduled vector store synchronization completed: {}", result);
        } catch (Exception e) {
            logger.error("Error during scheduled vector store synchronization", e);
        }
    }
    
    /**
     * Manually trigger a synchronization.
     * This method is thread-safe and prevents multiple synchronizations from running simultaneously.
     * 
     * @return The result of the synchronization, or null if a sync is already in progress
     */
    public SyncResult triggerSync() {
        if (syncInProgress.compareAndSet(false, true)) {
            try {
                logger.info("Starting manual vector store synchronization");
                SyncResult result = synchronizeVectorStore();
                logger.info("Manual vector store synchronization completed: {}", result);
                return result;
            } catch (Exception e) {
                logger.error("Error during manual vector store synchronization", e);
                return null;
            } finally {
                syncInProgress.set(false);
            }
        } else {
            logger.warn("Sync already in progress, ignoring trigger request");
            return null;
        }
    }
    
    /**
     * Synchronize the vector store with the database.
     * This method is protected by the syncInProgress flag to prevent multiple synchronizations.
     * 
     * @return The result of the synchronization
     */
    private SyncResult synchronizeVectorStore() {
        lastSyncTime = LocalDateTime.now();
        lastSyncResult = storageService.synchronizeVectorStore();
        return lastSyncResult;
    }
    
    /**
     * Check if a synchronization is currently in progress.
     * 
     * @return True if a sync is in progress, false otherwise
     */
    public boolean isSyncInProgress() {
        return syncInProgress.get();
    }
    
    /**
     * Get the time of the last synchronization.
     * 
     * @return The time of the last synchronization, or null if no sync has been performed
     */
    public String getLastSyncTime() {
        return lastSyncTime != null ? lastSyncTime.format(formatter) : "Never";
    }
    
    /**
     * Get the result of the last synchronization.
     * 
     * @return The result of the last synchronization, or null if no sync has been performed
     */
    public SyncResult getLastSyncResult() {
        return lastSyncResult;
    }
}
