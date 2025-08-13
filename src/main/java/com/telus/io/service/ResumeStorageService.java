package com.telus.io.service;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.telus.io.dto.ResumeParseResult;
import com.telus.io.dto.SyncResult;
import com.telus.io.model.Resume;


/**
 * Service for storing and retrieving resumes.
 */
public interface ResumeStorageService {
    
    /**
     * Store a resume.
     * 
     * @param parseResult The parsed resume data
     * @param file The uploaded file
     * @return The stored resume
     * @throws IOException If there is an error reading the file
     */
    Resume storeResume(ResumeParseResult parseResult, MultipartFile file) throws IOException;
    
    /**
     * Find a resume by name, email, and phone number.
     * 
     * @param name The name to search for
     * @param email The email to search for
     * @param phoneNumber The phone number to search for
     * @return The resume, if found
     */
    Optional<Resume> findByNameEmailPhone(String name, String email, String phoneNumber);
    
    /**
     * Update a resume.
     * 
     * @param id The ID of the resume to update
     * @param parseResult The parsed resume data
     * @return The updated resume
     */
    Resume updateResume(UUID id, ResumeParseResult parseResult);
    
    /**
     * Get a resume by ID.
     * 
     * @param id The ID of the resume to get
     * @return The resume, if found
     */
    Optional<Resume> getResumeById(UUID id);
    
    /**
     * Get all resumes with pagination.
     * 
     * @param pageable The pagination information
     * @return A page of resumes
     */
    Page<Resume> getAllResumes(Pageable pageable);
    
    /**
     * Delete a resume by ID.
     * 
     * @param id The ID of the resume to delete
     */
    void deleteResume(UUID id);
    
    /**
     * Synchronize the vector store with the database.
     * This ensures that:
     * 1. Every resume in the database has exactly one entry in the vector store
     * 2. There are no orphaned entries in the vector store
     * 3. There are no duplicate entries in the vector store
     * 
     * @return The result of the synchronization
     */
    SyncResult synchronizeVectorStore();
    
    public void saveToVectorStore(Resume resume) ;
}
