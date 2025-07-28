package com.telus.io.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.telus.io.dto.ResumeParseResult;
import com.telus.io.dto.SyncResult;
import com.telus.io.model.Resume;
import com.telus.io.repository.ResumeRepository;
import com.telus.io.service.ResumeStorageService;



/**
 * Implementation of ResumeStorageService that stores resumes in a database
 * and vector store. Uses multithreading for improved performance.
 * Simplified to focus on database operations and vector store synchronization.
 */
@Service
public class ResumeStorageServiceImpl implements ResumeStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(ResumeStorageServiceImpl.class);
    
    private final ResumeRepository resumeRepository;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    
    @Autowired
    @Qualifier("resumeProcessingExecutor")
    private Executor resumeProcessingExecutor;
    
    public ResumeStorageServiceImpl(
            ResumeRepository resumeRepository,
            @Qualifier("resumeVectorStore") VectorStore vectorStore,
            EmbeddingModel embeddingModel,
            JdbcTemplate jdbcTemplate) {
        this.resumeRepository = resumeRepository;
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public Optional<Resume> getResumeById(UUID id) {
        return resumeRepository.findById(id);
    }
    
    @Override
    public Page<Resume> getAllResumes(Pageable pageable) {
        return resumeRepository.findAll(pageable);
    }
    
    @Override
    public void deleteResume(UUID id) {
        // First delete from database in its own transaction
        deleteResumeFromDatabase(id);
        
        // Then try to delete from vector store (outside the database transaction)
        try {
            vectorStore.delete(List.of(id.toString()));
            logger.info("Deleted resume from vector store: {}", id);
        } catch (Exception e) {
            logger.error("Error deleting resume from vector store: {}", id, e);
            // Continue execution - we don't want to fail the entire operation if vector store fails
            // The resume is still deleted from the database
        }
    }
    
    /**
     * Delete a resume from the database in its own transaction.
     * 
     * @param id The ID of the resume to delete
     */
    @Transactional
    private void deleteResumeFromDatabase(UUID id) {
        try {
            // Delete from database
            resumeRepository.deleteById(id);
            logger.info("Deleted resume from database: {}", id);
        } catch (Exception e) {
            logger.error("Error deleting resume from database: {}", id, e);
            throw e; // Re-throw the exception to trigger transaction rollback
        }
    }
    
    /**
     * Save a resume to the vector store.
     * 
     * @param resume The resume to save
     */
    private void saveToVectorStore(Resume resume) {
        try {
            // Create a more comprehensive metadata map
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "resume");
            metadata.put("resumeId", resume.getId().toString());
            metadata.put("name", resume.getName());
            metadata.put("email", resume.getEmail());
            metadata.put("phoneNumber", resume.getPhoneNumber());
            
            // Add optional fields if available
            if (resume.getFileType() != null) {
                metadata.put("fileType", resume.getFileType());
            }
            
            if (resume.getOriginalFileName() != null) {
                metadata.put("originalFileName", resume.getOriginalFileName());
            }
            
            if (resume.getUploadedAt() != null) {
                metadata.put("uploadedAt", resume.getUploadedAt().toString());
            }
            
            if (resume.getUpdatedAt() != null) {
                metadata.put("updatedAt", resume.getUpdatedAt().toString());
            }
            
            Document document = new Document(resume.getFullText(), metadata);
            
            vectorStore.add(List.of(document));
            
            logger.info("Saved resume to vector store: {}", resume.getId());
        } catch (Exception e) {
            logger.error("Error saving resume to vector store: {}", resume.getId(), e);
            // Continue execution - we don't want to fail the entire operation if vector store fails
        }
    }
    
    @Override
    public SyncResult synchronizeVectorStore() {
        logger.info("Starting vector store synchronization");
        
        AtomicInteger duplicatesRemoved = new AtomicInteger(0);
        AtomicInteger missingAdded = new AtomicInteger(0);
        AtomicInteger orphansRemoved = new AtomicInteger(0);
        
        try {
            // Step 1: Get all resume IDs from the database (our source of truth)
            List<UUID> dbResumeIds = resumeRepository.findAllIds();
            logger.info("Found {} resumes in database", dbResumeIds.size());
            
            // Step 2: Process resumes in parallel
            List<CompletableFuture<Void>> futures = dbResumeIds.stream()
                    .map(resumeId -> processResumeAsync(resumeId, duplicatesRemoved, missingAdded))
                    .collect(Collectors.toList());
            
            // Wait for all futures to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            // Step 3: Find orphaned entries (can be done after all resumes are processed)
            orphansRemoved.set(removeOrphanedEntries(dbResumeIds));
            
        } catch (Exception e) {
            logger.error("Error during vector store synchronization", e);
        }
        
        logger.info("Vector store synchronization completed: {} duplicates removed, {} missing added, {} orphans removed",
                duplicatesRemoved.get(), missingAdded.get(), orphansRemoved.get());
        
        return new SyncResult(duplicatesRemoved.get(), missingAdded.get(), orphansRemoved.get());
    }
    
    /**
     * Process a resume asynchronously during synchronization.
     * 
     * @param resumeId The ID of the resume to process
     * @param duplicatesRemoved Counter for duplicates removed
     * @param missingAdded Counter for missing entries added
     * @return A CompletableFuture that will complete when the processing is done
     */
    @Async("resumeProcessingExecutor")
    public CompletableFuture<Void> processResumeAsync(UUID resumeId, AtomicInteger duplicatesRemoved, AtomicInteger missingAdded) {
        try {
            // Find all entries in vector store for this resume ID
            List<String> vectorEntries = findVectorEntriesByResumeId(resumeId.toString());
            
            if (vectorEntries.isEmpty()) {
                // Case: Missing in vector store - add it
                resumeRepository.findById(resumeId).ifPresent(resume -> {
                    saveToVectorStore(resume);
                    logger.info("Added missing vector entry for resume: {}", resumeId);
                    missingAdded.incrementAndGet();
                });
            } else if (vectorEntries.size() > 1) {
                // Case: Duplicates in vector store - keep only the first one
                // Delete all except the first entry
                for (int i = 1; i < vectorEntries.size(); i++) {
                    // Delete the duplicate entry directly from the database
                    jdbcTemplate.update(
                        "DELETE FROM resume_vector_store WHERE id = ?::uuid",
                        UUID.fromString(vectorEntries.get(i))
                    );
                    logger.info("Removed duplicate vector entry for resume: {}", resumeId);
                    duplicatesRemoved.incrementAndGet();
                }
            }
            // Case: Exactly one entry - this is correct, do nothing
        } catch (Exception e) {
            logger.error("Error processing resume ID: {}", resumeId, e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Remove orphaned entries from the vector store.
     * 
     * @param dbResumeIds List of valid resume IDs from the database
     * @return Number of orphaned entries removed
     */
    private int removeOrphanedEntries(List<UUID> dbResumeIds) {
        AtomicInteger orphansRemoved = new AtomicInteger(0);
        
        List<String> allVectorResumeIds = getAllVectorResumeIds();
        for (String vectorResumeId : allVectorResumeIds) {
            try {
                UUID resumeId = UUID.fromString(vectorResumeId);
                if (!dbResumeIds.contains(resumeId)) {
                    // This is an orphaned entry - delete it
                    jdbcTemplate.update(
                        "DELETE FROM resume_vector_store WHERE resume_id = ?::uuid",
                        resumeId
                    );
                    logger.info("Removed orphaned vector entry: {}", vectorResumeId);
                    orphansRemoved.incrementAndGet();
                }
            } catch (IllegalArgumentException e) {
                // Not a valid UUID, could be another type of document
                logger.warn("Found non-UUID resume ID in vector store: {}", vectorResumeId);
            }
        }
        
        return orphansRemoved.get();
    }
    
    /**
     * Find all vector store entries for a specific resume ID.
     * 
     * @param resumeId The resume ID to search for
     * @return A list of vector store entry IDs
     */
    private List<String> findVectorEntriesByResumeId(String resumeId) {
        // Direct SQL query to find entries with the specified resume ID
        return jdbcTemplate.queryForList(
            "SELECT id::text FROM resume_vector_store WHERE resume_id = ?::uuid",
            String.class,
            resumeId
        );
    }
    
    /**
     * Get all resume IDs from the vector store.
     * 
     * @return A list of all resume IDs in the vector store
     */
    private List<String> getAllVectorResumeIds() {
        // Direct SQL query to get all resume IDs
        return jdbcTemplate.queryForList(
            "SELECT resume_id::text FROM resume_vector_store",
            String.class
        );
    }

    @Override
    public Resume storeResume(ResumeParseResult parseResult, MultipartFile file) throws IOException {
        // First save to database in its own transaction
        Resume savedResume = saveResumeToDatabase(parseResult, file);
        
        // Then try to save to vector store (outside the database transaction)
        
        
        return savedResume;
    }
    /**
     * Save a resume to the database in its own transaction.
     * 
     * @param parseResult The parsed resume data
     * @param file The uploaded file
     * @return The saved resume
     * @throws IOException If there is an error reading the file
     */
    @Transactional
    private Resume saveResumeToDatabase(ResumeParseResult parseResult, MultipartFile file) throws IOException {
        // Check if resume already exists
        Optional<Resume> existingResume = findByNameEmailPhone(
                parseResult.getName(),
                parseResult.getEmail(),
                parseResult.getPhoneNumber()
        );
        
        if (existingResume.isPresent()) {
            // Update existing resume
            Resume resume = existingResume.get();
            resume.setFullText(parseResult.getFullText());
            resume.setFileType(parseResult.getFileType());
            resume.setOriginalFileName(file.getOriginalFilename());
            resume.setUpdatedAt(LocalDateTime.now());
            
            // Save to database
            return resumeRepository.save(resume);
        } else {
            // Create new resume
            Resume resume = new Resume(
                    parseResult.getName(),
                    parseResult.getEmail(),
                    parseResult.getPhoneNumber(),
                    parseResult.getFullText(),
                    parseResult.getFileType(),
                    file.getOriginalFilename()
            );
            
            // Save to database
            return resumeRepository.save(resume);
        }
    }

	 @Override
	    public Optional<Resume> findByNameEmailPhone(String name, String email, String phoneNumber) {
	        return resumeRepository.findByNameAndEmailAndPhoneNumber(name, email, phoneNumber);
	    }

	 @Override
	    public Resume updateResume(UUID id, ResumeParseResult parseResult) {
	        // First update in database in its own transaction
	        Resume savedResume = updateResumeInDatabase(id, parseResult);
	        
	       
	        
	        return savedResume;
	    }
	 /**
	     * Update a resume in the database in its own transaction.
	     * 
	     * @param id The ID of the resume to update
	     * @param parseResult The parsed resume data
	     * @return The updated resume
	     */
	    @Transactional
	    private Resume updateResumeInDatabase(UUID id, ResumeParseResult parseResult) {
	        Resume resume = resumeRepository.findById(id)
	                .orElseThrow(() -> new IllegalArgumentException("Resume not found with ID: " + id));
	        
	        resume.setName(parseResult.getName());
	        resume.setEmail(parseResult.getEmail());
	        resume.setPhoneNumber(parseResult.getPhoneNumber());
	        resume.setFullText(parseResult.getFullText());
	        resume.setUpdatedAt(LocalDateTime.now());
	        
	        // Save to database
	        return resumeRepository.save(resume);
	    }
}
