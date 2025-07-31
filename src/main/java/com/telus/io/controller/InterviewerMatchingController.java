package com.telus.io.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.telus.io.dto.request.JobDescriptionRequest;
import com.telus.io.dto.response.ApiResponse;
import com.telus.io.dto.response.InterviewerMatchResponse;
import com.telus.io.model.InterviewerProfile;
import com.telus.io.repository.InterviewerVectorStoreRepository;
import com.telus.io.service.InterviewerMatchingService;
import com.telus.io.service.ResumeMatchingService;



/**
 * REST controller for interviewer matching operations.
 */
@RestController
@RequestMapping("/api/interviewer-matching")
public class InterviewerMatchingController {
    
    private static final Logger logger = LoggerFactory.getLogger(InterviewerMatchingController.class);
    
    private final InterviewerMatchingService matchingService;
    private final ResumeMatchingService resumeMatchingService;
   private final InterviewerVectorStoreRepository vectorStoreRepository;
    private final RetryTemplate aiRetryTemplate;
    private final ChatModel chatModel;
    private final String interviewerMatchExplanationPrompt;
    private final String jobDescriptionMatchPrompt;
    
//    @Autowired
//    @Qualifier("interviewerVectorStoreService")
//    private InterviewerVectorStoreConfig.InterviewerVectorStoreService vectorStoreService;
    
    @Autowired
    public InterviewerMatchingController(
            InterviewerMatchingService matchingService,
            ResumeMatchingService resumeMatchingService,
            InterviewerVectorStoreRepository vectorStoreRepository,
            RetryTemplate aiRetryTemplate,
            ChatModel chatModel,
            @Qualifier("interviewerMatchExplanationPrompt") String interviewerMatchExplanationPrompt,
            @Qualifier("jobDescriptionMatchPrompt") String jobDescriptionMatchPrompt) {
        this.matchingService = matchingService;
		this.resumeMatchingService = resumeMatchingService;
        this.vectorStoreRepository = vectorStoreRepository;
        this.aiRetryTemplate = aiRetryTemplate;
        this.chatModel = chatModel;
        this.interviewerMatchExplanationPrompt = interviewerMatchExplanationPrompt;
        this.jobDescriptionMatchPrompt = jobDescriptionMatchPrompt;
    }
    
    
    @GetMapping("/resume/{resumeId}")
    public CompletableFuture<ResponseEntity<ApiResponse>> findInterviewersForCandidateAsync(
            @PathVariable UUID resumeId,
            @RequestParam(defaultValue = "5") int limit) {
        
        logger.info("Received async request to find interviewers for resume ID: {}", resumeId);
        
        return matchingService.findInterviewersWithExplanationsForCandidateAsync(resumeId, limit)
                .thenApply(responses -> {
                    ApiResponse apiResponse = new ApiResponse(
                            true, 
                            "Successfully found matching interviewers", 
                            responses);
                    
                    return ResponseEntity.ok(apiResponse);
                })
                .exceptionally(ex -> {
                    logger.error("Error finding interviewers for resume: {}", ex.getMessage(), ex);
                    
                    ApiResponse apiResponse = new ApiResponse(
                            false, 
                            "Error finding matching interviewers: " + ex.getMessage(), 
                            null);
                    
                    return ResponseEntity.ok(apiResponse);
                });
    }

    /**
     * Calculate a match score for an interviewer and job description.
     * This method gets the score directly from the AI response, eliminating the need
     * for vector similarity calculations and additional database calls.
     * 
     * @param matchExplanation The AI-generated match explanation that contains the score
     * @return A match score (0-100)
     */
    private int calculateMatchScore(String matchExplanation) {
        try {
            // Extract the score from the AI response
            int aiScore = extractScoreFromAIResponse(matchExplanation);
            
            // If we couldn't extract a score, use a default score
            if (aiScore == -1) {
                logger.warn("Could not extract score from AI response, using default score");
                return 75; // Default score
            }
            
            // Return the AI-provided score
            return Math.min(100, Math.max(0, aiScore)); // Ensure score is within 0-100 range
        } catch (Exception e) {
            logger.error("Error calculating match score: {}", e.getMessage(), e);
            // Fallback to a default score if calculation fails
            return 75;
        }
    }

    
    /**
     * Extract a score from the AI response.
     * 
     * @param response The AI response
     * @return A score (0-100), or -1 if no score could be extracted
     */
    private int extractScoreFromAIResponse(String response) {
        try {
            // First, look for our specific format "Final Match Score: X%"
            java.util.regex.Pattern finalScorePattern = java.util.regex.Pattern.compile("Final Match Score:\\s*(\\d{1,3})%");
            java.util.regex.Matcher finalScoreMatcher = finalScorePattern.matcher(response);
            
            if (finalScoreMatcher.find()) {
                return Integer.parseInt(finalScoreMatcher.group(1));
            }
            
            // If we can't find the specific format, look for other patterns like "Match Score: 85%" or "75/100"
            java.util.regex.Pattern generalPattern = java.util.regex.Pattern.compile("\\b(\\d{1,3})\\s*[/%]");
            java.util.regex.Matcher generalMatcher = generalPattern.matcher(response);
            
            if (generalMatcher.find()) {
                return Integer.parseInt(generalMatcher.group(1));
            }
            
            // If we can't find a score, check for the match status
            if (response.contains("MATCH: Highly recommended")) {
                return 90; // Assign a high score for strong recommendations
            } else if (response.contains("MATCH: Recommended")) {
                return 75; // Assign a medium score for recommendations
            } else if (response.contains("NOT A MATCH")) {
                return 40; // Assign a low score for non-matches
            }
            
            // If we can't extract a score, return -1
            return -1;
        } catch (Exception e) {
            logger.error("Error extracting score from AI response: {}", e.getMessage(), e);
            return -1;
        }
    }
    
    /**
     * Determine the match status based on the explanation content and score.
     * 
     * @param explanation The match explanation
     * @param matchScore The match score (0-100)
     * @return The match status (STRONG_MATCH, MATCH, or CONSIDER)
     */
    private InterviewerMatchResponse.MatchStatus determineMatchStatus(String explanation, int matchScore) {
        if (matchScore >= 85) {
            return InterviewerMatchResponse.MatchStatus.STRONG_MATCH;
        } else if (matchScore >= 70) {
            return InterviewerMatchResponse.MatchStatus.MATCH;
        } else {
            return InterviewerMatchResponse.MatchStatus.CONSIDER;
        }
    }
    
    
    
    
    
    
    @PostMapping("/job-description")
    public CompletableFuture<ResponseEntity<ApiResponse>> findInterviewersForJobDescriptionAsync(
            @RequestBody JobDescriptionRequest request,
            @RequestParam(defaultValue = "5") int limit) {
        
        long startTime = System.currentTimeMillis();
        logger.info("🔍 Starting async interviewer search for job description (limit: {})", limit);
        
        // Validate and cap the limit
        if (limit > 10) {
            limit = 10;
            logger.warn("⚠️ Limit capped at 10 to prevent excessive AI calls");
        }
        
        String jobDescription = request.getJobDescription();
        
        // Check if vector store is empty
        if (!vectorStoreRepository.existsAny()) {
            logger.warn("❌ No interviewer vector store entries found");
            ApiResponse apiResponse = new ApiResponse(false, 
                "No interviewer vector store entries found. Please sync the vector store first.", 
                null);
            return CompletableFuture.completedFuture(ResponseEntity.ok(apiResponse));
        }
        
        // Step 1: Find interviewers by expertise
        List<InterviewerProfile> interviewers = matchingService.findInterviewersByExpertise(
            jobDescription, limit * 2);
        
        logger.info("📊 Found {} potential interviewers, processing top {}", 
                    interviewers.size(), Math.min(limit, interviewers.size()));
        
        // Step 2: Process in parallel
        final int finalLimit = limit;
        return CompletableFuture.supplyAsync(() -> {
            
            // Create parallel futures
            List<CompletableFuture<InterviewerMatchResponse>> futures = interviewers.stream()
                .limit(finalLimit)
                .map(interviewer -> processInterviewerAsync(interviewer, jobDescription))
                .collect(Collectors.toList());
            
            logger.info("🚀 Started {} parallel AI processing tasks", futures.size());
            
            // Wait for all futures and handle results
            List<InterviewerMatchResponse> responses = waitForAllFutures(futures);
            
            long totalTime = System.currentTimeMillis() - startTime;
            logger.info("✅ Completed {} interviewer matches in {}ms", responses.size(), totalTime);
            
            return responses;
            
        }).thenApply(responses -> {
            ApiResponse apiResponse = new ApiResponse(
                    true, 
                    String.format("Successfully found %d matching interviewers", responses.size()), 
                    responses);
            return ResponseEntity.ok(apiResponse);
            
        }).exceptionally(ex -> {
            long totalTime = System.currentTimeMillis() - startTime;
            logger.error("❌ Error in parallel processing after {}ms: {}", totalTime, ex.getMessage(), ex);
            
            ApiResponse apiResponse = new ApiResponse(
                    false, 
                    "Error finding matching interviewers: " + ex.getMessage(), 
                    null);
            return ResponseEntity.ok(apiResponse);
        });
    }

    /**
     * Wait for all futures to complete and handle timeouts/errors gracefully.
     * This method fixes the type inference issue.
     */
    private List<InterviewerMatchResponse> waitForAllFutures(
            List<CompletableFuture<InterviewerMatchResponse>> futures) {
        
        List<InterviewerMatchResponse> responses = new ArrayList<>();
        
        for (CompletableFuture<InterviewerMatchResponse> future : futures) {
            try {
                InterviewerMatchResponse response = future.get(30, TimeUnit.SECONDS);
                if (response != null) {
                    responses.add(response);
                }
            } catch (TimeoutException e) {
                logger.error("⏰ AI call timed out for interviewer");
                InterviewerMatchResponse fallback = createFallbackResponse(null, "AI analysis timed out");
                responses.add(fallback);
            } catch (Exception e) {
                logger.error("❌ AI call failed for interviewer: {}", e.getMessage());
                InterviewerMatchResponse fallback = createFallbackResponse(null, "AI analysis failed: " + e.getMessage());
                responses.add(fallback);
            }
        }
        
        // Sort by match score (highest first)
        responses.sort((r1, r2) -> Integer.compare(r2.getMatchScore(), r1.getMatchScore()));
        
        return responses;
    }
    /**
     * Process a single interviewer asynchronously with AI analysis.
     * This method runs in parallel with other interviewer processing.
     * 
     * @param interviewer The interviewer to process
     * @param jobDescription The job description to match against
     * @return CompletableFuture containing the match response
     */
    private CompletableFuture<InterviewerMatchResponse> processInterviewerAsync(
            InterviewerProfile interviewer, 
            String jobDescription) {
        
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                logger.debug("🔄 Processing interviewer: {} (ID: {})", 
                            interviewer.getName(), interviewer.getId());
                
                // Create the response object with basic info
                InterviewerMatchResponse response = new InterviewerMatchResponse();
                response.setInterviewerId(interviewer.getId());
                response.setName(interviewer.getName());
                response.setEmail(interviewer.getEmail());
                response.setExperienceYears(interviewer.getExperienceYears());
                response.setTechnicalExpertise(interviewer.getTechnicalExpertise());
                response.setSpecializations(interviewer.getSpecializations());
                
                // Generate AI match explanation (this is the expensive operation)
                String matchExplanation = generateMatchExplanationForJobDescription(
                    interviewer, jobDescription);
                response.setMatchExplanation(matchExplanation);
                
                // Calculate match score from AI response
                int matchScore = calculateMatchScore(matchExplanation);
                response.setMatchScore(matchScore);
                
                // Determine match status
                InterviewerMatchResponse.MatchStatus matchStatus = 
                    determineMatchStatus(matchExplanation, matchScore);
                response.setMatchStatus(matchStatus);
                
                long processingTime = System.currentTimeMillis() - startTime;
                logger.debug("✅ Completed processing {} in {}ms (score: {})", 
                            interviewer.getName(), processingTime, matchScore);
                
                return response;
                
            } catch (Exception e) {
                long processingTime = System.currentTimeMillis() - startTime;
                logger.error("❌ Error processing interviewer {} after {}ms: {}", 
                            interviewer.getName(), processingTime, e.getMessage(), e);
                
                // Return fallback response instead of null
                return createFallbackResponse(interviewer, 
                    "Error during AI analysis: " + e.getMessage());
            }
        });
    }
    /**
     * Create a fallback response when AI processing fails.
     * 
     * @param interviewer The interviewer (can be null)
     * @param errorMessage The error message
     * @return A basic response with default values
     */
    private InterviewerMatchResponse createFallbackResponse(
            InterviewerProfile interviewer, 
            String errorMessage) {
        
        InterviewerMatchResponse response = new InterviewerMatchResponse();
        
        if (interviewer != null) {
            response.setInterviewerId(interviewer.getId());
            response.setName(interviewer.getName());
            response.setEmail(interviewer.getEmail());
            response.setExperienceYears(interviewer.getExperienceYears());
            response.setTechnicalExpertise(interviewer.getTechnicalExpertise());
            response.setSpecializations(interviewer.getSpecializations());
        }
        
        response.setMatchExplanation("AI analysis unavailable: " + errorMessage);
        response.setMatchScore(50);  // Default neutral score
        response.setMatchStatus(InterviewerMatchResponse.MatchStatus.CONSIDER);
        
        return response;
    }
    /**
     * Generate a match explanation for an interviewer and job description.
     * Enhanced with better error handling and timeout protection.
     */
    private String generateMatchExplanationForJobDescription(
            InterviewerProfile interviewer, 
            String jobDescription) {
        
        try {
            // Create the prompt
            String interviewerName = interviewer.getName();
            int interviewerExperience = interviewer.getExperienceYears();
            List<String> interviewerExpertise = interviewer.getTechnicalExpertise();
            List<String> interviewerSpecializations = interviewer.getSpecializations();
            
            String prompt = jobDescriptionMatchPrompt
                    .replace("{{interviewer_name}}", interviewerName)
                    .replace("{{interviewer_experience}}", String.valueOf(interviewerExperience))
                    .replace("{{interviewer_expertise}}", 
                            interviewerExpertise != null ? String.join(", ", interviewerExpertise) : "")
                    .replace("{{interviewer_specializations}}", 
                            interviewerSpecializations != null ? String.join(", ", interviewerSpecializations) : "")
                    .replace("{{job_description}}", jobDescription)
                    .replace("{{match_percentage}}", "0");
            
            // Make AI call with retry logic
            return aiRetryTemplate.execute(context -> {
                if (context.getRetryCount() > 0) {
                    logger.debug("🔄 Retry attempt {} for interviewer {}", 
                               context.getRetryCount(), interviewerName);
                }
                
                try {
                    String result = chatModel.call(prompt);
                    logger.debug("✅ AI call successful for interviewer {}", interviewerName);
                    return result;
                } catch (Exception e) {
                    logger.warn("⚠️ AI call failed for interviewer {}: {}", 
                               interviewerName, e.getMessage());
                    throw new RuntimeException("AI service error", e);
                }
            });
            
        } catch (Exception e) {
            logger.error("❌ Failed to generate AI explanation for interviewer {}: {}", 
                        interviewer.getName(), e.getMessage(), e);
            
            // Return fallback explanation
            return String.format(
                "Interviewer %s has %d years of experience with expertise in %s. " +
                "Manual review recommended due to AI service unavailability. " +
                "Final Match Score: 75%%",
                interviewer.getName(),
                interviewer.getExperienceYears(),
                interviewer.getTechnicalExpertise() != null ? 
                    String.join(", ", interviewer.getTechnicalExpertise()) : "various technologies"
            );
        }
    }

    
    /**
     * Find suitable interviewers for a candidate based on their evaluation.
     * 
     * @param evaluationId The ID of the candidate's evaluation
     * @param limit The maximum number of interviewers to return (optional, default 5)
     * @return A list of interviewer match responses ordered by relevance
     */
    @GetMapping("/evaluations/{evaluationId}")
    public ResponseEntity<?> findInterviewersForEvaluation(
            @PathVariable UUID evaluationId,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "false") boolean includeNonMatches) {
        
        // Check if vector store is empty
        if (!vectorStoreRepository.existsAny()) {
            logger.warn("No interviewer vector store entries found when searching for evaluation: {}", evaluationId);
            return ResponseEntity.ok(
                new ApiResponse(false, 
                    "No interviewer vector store entries found. Please sync the vector store first by visiting /interviewer-sync or calling the sync API endpoint.", 
                    null));
        }
        
        List<InterviewerMatchResponse> interviewers = matchingService.findInterviewersForEvaluation(evaluationId, limit, includeNonMatches);
        
        return ResponseEntity.ok(interviewers);
    }
    
    
    /**
     * Find suitable interviewers for a job description.
     * 
     * @param query The job description or expertise query
     * @param limit The maximum number of interviewers to return (optional, default 5)
     * @return A list of interviewer profiles ordered by relevance
     */
    @GetMapping("/expertise")
    public ResponseEntity<?> findInterviewersByExpertise(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {
        
        // Check if vector store is empty
        if (!vectorStoreRepository.existsAny()) {
            logger.warn("No interviewer vector store entries found when searching by expertise: {}", query);
            return ResponseEntity.ok(
                new ApiResponse(false, 
                    "No interviewer vector store entries found. Please sync the vector store first by visiting /interviewer-sync or calling the sync API endpoint.", 
                    null));
        }
        
        List<InterviewerProfile> interviewers = matchingService.findInterviewersByExpertise(query, limit);
        return ResponseEntity.ok(interviewers);
    }
    
    /**
     * Find suitable interviewers with additional filters.
     * 
     * @param query The job description or expertise query
     * @param filters Additional filters to apply (e.g., minimum experience, tier)
     * @param limit The maximum number of interviewers to return (optional, default 5)
     * @return A list of interviewer profiles ordered by relevance
     */
    @PostMapping("/filtered")
    public ResponseEntity<?> findInterviewersWithFilters(
            @RequestParam String query,
            @RequestBody Map<String, String> filters,
            @RequestParam(defaultValue = "5") int limit) {
        
        // Check if vector store is empty
        if (!vectorStoreRepository.existsAny()) {
            logger.warn("No interviewer vector store entries found when searching with filters: {}", query);
            return ResponseEntity.ok(
                new ApiResponse(false, 
                    "No interviewer vector store entries found. Please sync the vector store first by visiting /interviewer-sync or calling the sync API endpoint.", 
                    null));
        }
        
        List<InterviewerProfile> interviewers = matchingService.findInterviewersWithFilters(query, filters, limit);
        return ResponseEntity.ok(interviewers);
    }
    
    /**
     * Check if an interviewer is available on a specific date.
     * 
     * @param interviewerId The ID of the interviewer
     * @param date The date to check (in format YYYY-MM-DD)
     * @return true if the interviewer has available slots on the specified date
     */
    @GetMapping("/availability/{interviewerId}")
    public ResponseEntity<ApiResponse> isInterviewerAvailable(
            @PathVariable UUID interviewerId,
            @RequestParam String date) {
        boolean isAvailable = matchingService.isInterviewerAvailable(interviewerId, date);
        return ResponseEntity.ok(new ApiResponse(true, "Availability checked successfully", isAvailable));
    }
    /**
     * Generate a summary of why an interviewer is a good match for a candidate.
     * 
     * @param interviewerId The ID of the interviewer
     * @param resumeId The ID of the candidate's resume
     * @return A summary explaining the match
     */
    @GetMapping("/summary/{interviewerId}/{resumeId}")
    public ResponseEntity<ApiResponse> generateMatchSummary(
            @PathVariable UUID interviewerId,
            @PathVariable UUID resumeId) {
        String summary = matchingService.generateMatchSummary(interviewerId, resumeId);
        return ResponseEntity.ok(new ApiResponse(true, "Match summary generated successfully", summary));
    }
    
}
