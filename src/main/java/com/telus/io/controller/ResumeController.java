package com.telus.io.controller;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.telus.io.dto.ResumeParseResult;
import com.telus.io.dto.response.ResumeResponse;
import com.telus.io.exception.ResourceNotFoundException;
import com.telus.io.model.Resume;
import com.telus.io.model.ResumeMatch;
import com.telus.io.service.ResumeMatchingService;
import com.telus.io.service.ResumeParserService;
import com.telus.io.service.ResumeStorageService;

/**
 * Controller for resume matching endpoints. Simplified to focus only on
 * matching functionality without file uploads. Uses synchronous endpoints with
 * optimized internal processing.
 * 
 * @param <ResumeParserService>
 */
@RestController
@RequestMapping("/api/resumes")

public class ResumeController {

	private static final Logger logger = LoggerFactory.getLogger(ResumeController.class);

	private final ResumeStorageService storageService;
	private final ResumeMatchingService matchingService;
	private final ResumeParserService parserService;

	public ResumeController(ResumeParserService parserService, ResumeStorageService storageService,
			ResumeMatchingService matchingService) {
		this.storageService = storageService;
		this.matchingService = matchingService;
		this.parserService = parserService;
	}

	/**
	 * Upload a resume.
	 * 
	 * @param file The resume file to upload
	 * @return The uploaded resume
	 */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResumeResponse> uploadResume(@RequestParam("file") MultipartFile file) throws IOException {
		logger.info("Uploading resume: {}", file.getOriginalFilename());

		// Parse the resume
		ResumeParseResult parseResult = parserService.parseResume(file);

		// Store the resume
		Resume resume = storageService.storeResume(parseResult, file);

		// Return the response
		ResumeResponse response = new ResumeResponse(resume);

		logger.info("Resume uploaded successfully: {}", resume.getId());
		
		if (response.getId() == null) {
			logger.error("Resume upload failed, no ID returned");
			return ResponseEntity.status(500).build();
		}else {
			logger.info("Resume uploaded successfully with ID: {}", response.getId());
			
			logger.info("saving to vectore: {}", response.getId());
			// Save to vector store
			storageService.saveToVectorStore(resume);
			
		}
		
		

		return ResponseEntity.ok(response);
	}

	@PostMapping("/match-new")
	public ResponseEntity<List<ResumeMatch>> matchResumes_new(@RequestParam("jd") String jobDescription,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		logger.info("Matching resumes to job description, limit: {}", limit);

		// Find matching resumes - this method is internally optimized for parallel
		// processing
		List<ResumeMatch> matches = matchingService.findMatchingResumes(jobDescription, limit);

		// Convert to response objects with match information

		logger.info("Found {} matching resumes", matches.size());

		return ResponseEntity.ok(matches);
	}

	/**
	 * Get a resume by ID.
	 * 
	 * @param id The ID of the resume to get
	 * @return The resume
	 */
	@GetMapping("/{id}")
	public ResponseEntity<ResumeResponse> getResume(@PathVariable UUID id) {
		logger.info("Getting resume: {}", id);

		Resume resume = storageService.getResumeById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Resume", "id", id));

		ResumeResponse response = new ResumeResponse(resume);
		return ResponseEntity.ok(response);
	}

}
