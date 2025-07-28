package com.telus.io.controller;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

	public ResumeController(ResumeParserService parserService,
			ResumeStorageService storageService, ResumeMatchingService matchingService) {
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

		return ResponseEntity.ok(response);
	}

	/**
	 * Match resumes to a job description. This endpoint is synchronous but uses
	 * optimized internal parallel processing.
	 * 
	 * @param jobDescription The job description to match against
	 * @param limit          The maximum number of matches to return
	 * @return A list of resume matches
	 */
	@PostMapping("/match")
	public ResponseEntity<List<ResumeResponse>> matchResumes(@RequestParam("jd") String jobDescription,
			@RequestParam(value = "limit", defaultValue = "5") int limit) {
		logger.info("Matching resumes to job description, limit: {}", limit);

		// Find matching resumes - this method is internally optimized for parallel
		// processing
		List<ResumeMatch> matches = matchingService.findMatchingResumes(jobDescription, limit);

		// Convert to response objects with match information
		List<ResumeResponse> responses = matches.stream().map(match -> {
			ResumeResponse response = new ResumeResponse(match.getResume());
			response.setMatchScore(match.getScore());
			response.setMatchExplanation(match.getExplanation());
			return response;
		}).collect(Collectors.toList());

		logger.info("Found {} matching resumes", responses.size());

		return ResponseEntity.ok(responses);
	}
	
	@PostMapping("/match-new")
	public ResponseEntity<List<ResumeMatch>> matchResumes_new(@RequestParam("jd") String jobDescription,
			@RequestParam(value = "limit", defaultValue = "100") int limit) {
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

	/**
	 * Get detailed match information for a resume and job description. This
	 * endpoint is synchronous but uses the optimized async method internally.
	 * 
	 * @param id             The ID of the resume to match
	 * @param jobDescription The job description to match against
	 * @return The match information
	 */
//	@GetMapping("/{id}/match")
//	public ResponseEntity<ResumeMatch> getResumeMatch(@PathVariable UUID id,
//			@RequestParam("jd") String jobDescription) {
//		logger.info("Getting match for resume: {} and job description", id);
//
//		Resume resume = storageService.getResumeById(id)
//				.orElseThrow(() -> new ResourceNotFoundException("Resume", "id", id));
//
//		try {
//			// Call the async method and wait for the result
//			String explanation = matchingService.explainMatchAsync(resume, jobDescription).join();
//
//			// Extract score from the explanation
//			int score = 0;
//			if (matchingService instanceof ResumeMatchingServiceImpl) {
//				score = ((ResumeMatchingServiceImpl) matchingService).extractScoreFromExplanation(explanation);
//			}
//
//			ResumeMatch match = new ResumeMatch(resume, score, explanation);
//			return ResponseEntity.ok(match);
//		} catch (Exception e) {
//			// Convert to appropriate application exception
//			throw ExceptionUtils.handleAiServiceException(e, "generating match for resume: " + id);
//		}
//	}
}
