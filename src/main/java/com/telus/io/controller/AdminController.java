package com.telus.io.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telus.io.dto.SyncResult;
import com.telus.io.exception.SyncInProgressException;
import com.telus.io.scheduler.VectorStoreSyncScheduler;



/**
 * Controller for administrative operations. Enhanced to focus on sync
 * operations and monitoring.
 */
@RestController
@RequestMapping("/api/admin")


public class AdminController {

	private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

	private final VectorStoreSyncScheduler syncScheduler;

	public AdminController(VectorStoreSyncScheduler syncScheduler) {
		this.syncScheduler = syncScheduler;
	}

	/**
	 * Synchronize the vector store with the database. This ensures that: 1. Every
	 * resume in the database has exactly one entry in the vector store 2. There are
	 * no orphaned entries in the vector store 3. There are no duplicate entries in
	 * the vector store
	 * 
	 * @return The result of the synchronization
	 */
	@PostMapping("/sync-vector-store")
	public ResponseEntity<Map<String, Object>> syncVectorStore() {
		logger.info("Received request to synchronize vector store");

		// Check if sync is already in progress
		if (syncScheduler.isSyncInProgress()) {
			throw new SyncInProgressException("Vector store synchronization is already in progress");
		}

		// Trigger sync
		SyncResult result = syncScheduler.triggerSync();

		if (result == null) {
			throw new IllegalStateException("Failed to start vector store synchronization");
		}

		Map<String, Object> response = Map.of("success", true, "duplicatesRemoved", result.getDuplicatesRemoved(),
				"missingAdded", result.getMissingAdded(), "orphansRemoved", result.getOrphansRemoved());

		logger.info("Vector store synchronization completed: {}", result);

		return ResponseEntity.ok(response);
	}

	/**
	 * Get the status of the vector store synchronization.
	 * 
	 * @return The status of the synchronization
	 */
	@GetMapping("/sync-status")
	public ResponseEntity<Map<String, Object>> getSyncStatus() {
		logger.info("Received request to get sync status");

		Map<String, Object> response = new HashMap<>();
		response.put("inProgress", syncScheduler.isSyncInProgress());
		response.put("lastSyncTime", syncScheduler.getLastSyncTime());

		SyncResult lastResult = syncScheduler.getLastSyncResult();
		if (lastResult != null) {
			response.put("lastSyncResult", Map.of("duplicatesRemoved", lastResult.getDuplicatesRemoved(),
					"missingAdded", lastResult.getMissingAdded(), "orphansRemoved", lastResult.getOrphansRemoved()));
		} else {
			response.put("lastSyncResult", null);
		}

		return ResponseEntity.ok(response);
	}
}
