package com.telus.io.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.telus.io.model.Resume;
import com.telus.io.model.ResumeMatch;


/**
 * Service for matching resumes to job descriptions.
 */
public interface ResumeMatchingService {
    
    /**
     * Find resumes that match a job description.
     * 
     * @param jobDescription The job description to match against
     * @param limit The maximum number of matches to return
     * @return A list of resume matches, ordered by relevance
     */
    List<ResumeMatch> findMatchingResumes(String jobDescription, int limit);
    
    /**
     * Explain why a resume matches a job description.
     * 
     * @param resume The resume to explain
     * @param jobDescription The job description to match against
     * @return An explanation of the match
     */
    String explainMatch(Resume resume, String jobDescription);
    
    /**
     * Asynchronously explain why a resume matches a job description.
     * This method is optimized for parallel processing.
     * 
     * @param resume The resume to explain
     * @param jobDescription The job description to match against
     * @return A CompletableFuture that will contain the explanation when complete
     */
    CompletableFuture<String> explainMatchAsync(Resume resume, String jobDescription);
}
