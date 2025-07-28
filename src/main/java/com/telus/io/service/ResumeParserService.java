package com.telus.io.service;

import org.springframework.web.multipart.MultipartFile;

import com.telus.io.dto.ResumeParseResult;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for parsing resumes from various file formats.
 * Supports both synchronous and asynchronous processing.
 */
public interface ResumeParserService {
    
    /**
     * Parse a resume from a file (synchronous).
     * 
     * @param file The file to parse
     * @return The parsed resume data
     * @throws IOException If there is an error reading the file
     */
    ResumeParseResult parseResume(MultipartFile file) throws IOException;
    
    /**
     * Parse a resume from a file asynchronously.
     * 
     * @param file The file to parse
     * @return A CompletableFuture that will contain the parsed resume data when complete
     */
    CompletableFuture<ResumeParseResult> parseResumeAsync(MultipartFile file);
    
    /**
     * Parse multiple resumes in parallel.
     * 
     * @param files The files to parse
     * @return A list of CompletableFuture objects that will contain the parsed resume data when complete
     */
    List<CompletableFuture<ResumeParseResult>> parseResumesInParallel(List<MultipartFile> files);
}
