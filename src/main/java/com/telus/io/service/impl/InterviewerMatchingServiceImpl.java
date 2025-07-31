package com.telus.io.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telus.io.config.InterviewerMatchingConfig;
import com.telus.io.config.InterviewerVectorStoreConfig.InterviewerVectorStoreService;
import com.telus.io.dto.response.InterviewerMatchResponse;
import com.telus.io.exception.ResourceNotFoundException;
import com.telus.io.model.CandidateEvaluationModel;
import com.telus.io.model.InterviewerProfile;
import com.telus.io.model.Resume;
import com.telus.io.model.ResumeAnalysis;
import com.telus.io.model.ResumeMatch;
import com.telus.io.repository.CandidateEvaluationRepository;
import com.telus.io.repository.InterviewerProfileRepository;
import com.telus.io.repository.InterviewerVectorStoreRepository;
import com.telus.io.repository.ResumeRepository;
import com.telus.io.service.InterviewerMatchingService;
import com.telus.io.service.InterviewerProfileService;
import com.telus.io.service.ResumeStorageService;



/**
 * Implementation of the InterviewerMatchingService interface.
 */
@Service
public class InterviewerMatchingServiceImpl implements InterviewerMatchingService {
    
    private static final Logger logger = LoggerFactory.getLogger(InterviewerMatchingServiceImpl.class);
    
    private final InterviewerProfileService interviewerService;
    private final InterviewerProfileRepository interviewerRepository;
   // private final InterviewerVectorStoreRepository vectorStoreRepository;
    private final InterviewerVectorStoreService vectorStoreService;
    private final ResumeRepository resumeRepository;
   // private final ResumeStorageService resumeStorageService;
    private final RetryTemplate aiRetryTemplate;
    private final InterviewerMatchingConfig matchingConfig;
    private final CandidateEvaluationRepository evaluationRepository;
    private final ChatModel chatModel;
    private final String interviewerMatchExplanationPrompt;
    
    @Autowired
    public InterviewerMatchingServiceImpl(
            InterviewerProfileService interviewerService,
            InterviewerProfileRepository interviewerRepository,
            InterviewerVectorStoreRepository vectorStoreRepository,
            @Qualifier("interviewerVectorStoreService") InterviewerVectorStoreService vectorStoreService,
            ResumeRepository resumeRepository,
            ResumeStorageService resumeStorageService,
            RetryTemplate aiRetryTemplate,
            InterviewerMatchingConfig matchingConfig,
            CandidateEvaluationRepository evaluationRepository,
            ChatModel chatModel,
            @Qualifier("interviewerMatchExplanationPrompt") String interviewerMatchExplanationPrompt) {
        this.interviewerService = interviewerService;
        this.interviewerRepository = interviewerRepository;
      //  this.vectorStoreRepository = vectorStoreRepository;
        this.vectorStoreService = vectorStoreService;
        this.resumeRepository = resumeRepository;
       // this.resumeStorageService = resumeStorageService;
        this.aiRetryTemplate = aiRetryTemplate;
        this.matchingConfig = matchingConfig;
        this.evaluationRepository = evaluationRepository;
        this.chatModel = chatModel;
        this.interviewerMatchExplanationPrompt = interviewerMatchExplanationPrompt;
    }
    
    @Override
    public List<InterviewerProfile> findInterviewersForCandidate(UUID resumeId, int limit) {
        try {
            // Get the similarity threshold from configuration
            final double similarityThreshold = matchingConfig.getSimilarityThreshold();
            
            // Check if any vector store entries exist using the JDBC service
            if (!vectorStoreService.existsAny()) {
                logger.warn("No interviewer vector store entries found. Vector store may need to be synced.");
                return new ArrayList<>();
            }
            
            Resume resume = resumeRepository.findById(resumeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId));
            
            // Extract key information from the resume
            String fullText = resume.getFullText();
            
            // Create a query based on the candidate's resume text
            String query = "Resume content: " + fullText;
            
            // Generate embedding for the query
            float[] queryEmbedding = vectorStoreService.generateEmbedding(query);
            String vectorString = vectorStoreService.convertToVectorString(queryEmbedding);
            
            // Find interviewers with matching expertise and get similarity scores
            // Request more results than needed to allow for filtering
            List<Map<String, Object>> results = vectorStoreService.findSimilarWithScores(vectorString, limit * 2);
            
            // Filter by similarity threshold and extract interviewer profiles
            List<InterviewerProfile> interviewers = results.stream()
                    .filter(result -> (Double)result.get("similarity") >= similarityThreshold)
                    .map(result -> (InterviewerProfile)result.get("interviewer"))
                    .limit(limit)
                    .collect(Collectors.toList());
            
            // Log the result
            if (interviewers.isEmpty()) {
                logger.warn("No matching interviewers found for resume ID: {} (similarity threshold: {})", 
                           resumeId, similarityThreshold);
            } else {
                logger.info("Found {} matching interviewers for resume ID: {} (similarity threshold: {})", 
                           interviewers.size(), resumeId, similarityThreshold);
            }
            
            return interviewers;
        } catch (Exception e) {
            logger.error("Error finding interviewers for candidate: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<InterviewerProfile> findInterviewersForMatch(ResumeMatch resumeMatch, int limit) {
        try {
            // Get the similarity threshold from configuration
            final double similarityThreshold = matchingConfig.getSimilarityThreshold();
            
            // Check if any vector store entries exist using the JDBC service
            if (!vectorStoreService.existsAny()) {
                logger.warn("No interviewer vector store entries found. Vector store may need to be synced.");
                return new ArrayList<>();
            }
            
            // Extract key information from the resume match
            Resume resume = resumeMatch.getResume();
            String explanation = resumeMatch.getExplanation();
            ResumeAnalysis analysis = resumeMatch.getAnalysis();
            
            // Create a query based on the match explanation and resume content
            String fullText = resume.getFullText();
            String query = "Resume content: " + fullText + ". Match explanation: " + explanation;
            
            // Generate embedding for the query
            float[] queryEmbedding = vectorStoreService.generateEmbedding(query);
            String vectorString = vectorStoreService.convertToVectorString(queryEmbedding);
            
            // Find interviewers with matching expertise and get similarity scores
            List<Map<String, Object>> results = vectorStoreService.findSimilarWithScores(vectorString, limit * 2);
            
            // Filter by similarity threshold and extract interviewer profiles
            List<InterviewerProfile> interviewers = results.stream()
                    .filter(result -> (Double)result.get("similarity") >= similarityThreshold)
                    .map(result -> (InterviewerProfile)result.get("interviewer"))
                    .limit(limit)
                    .collect(Collectors.toList());
            
            // Log the result
            if (interviewers.isEmpty()) {
                logger.warn("No matching interviewers found for resume match with resume ID: {} (similarity threshold: {})", 
                           resume.getId(), similarityThreshold);
            } else {
                logger.info("Found {} matching interviewers for resume match with resume ID: {} (similarity threshold: {})", 
                           interviewers.size(), resume.getId(), similarityThreshold);
            }
            
            return interviewers;
        } catch (Exception e) {
            logger.error("Error finding interviewers for match: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<InterviewerProfile> findInterviewersByExpertise(String query, int limit) {
        try {
            // Get the similarity threshold from configuration
            final double similarityThreshold = matchingConfig.getSimilarityThreshold();
            
            // Check if any vector store entries exist using the JDBC service
            if (!vectorStoreService.existsAny()) {
                logger.warn("No interviewer vector store entries found. Vector store may need to be synced.");
                return new ArrayList<>();
            }
            
            // Generate embedding for the query
            float[] queryEmbedding = vectorStoreService.generateEmbedding(query);
            String vectorString = vectorStoreService.convertToVectorString(queryEmbedding);
            
            // Find interviewers with matching expertise and get similarity scores
            List<Map<String, Object>> results = vectorStoreService.findSimilarWithScores(vectorString, limit * 2);
            
            // Filter by similarity threshold and extract interviewer profiles
            List<InterviewerProfile> interviewers = results.stream()
                    .filter(result -> (Double)result.get("similarity") >= similarityThreshold)
                    .map(result -> (InterviewerProfile)result.get("interviewer"))
                    .limit(limit)
                    .collect(Collectors.toList());
            
            // Log the result
            if (interviewers.isEmpty()) {
                logger.warn("No matching interviewers found for expertise query: {} (similarity threshold: {})", 
                           query, similarityThreshold);
            } else {
                logger.info("Found {} matching interviewers for expertise query (similarity threshold: {})", 
                           interviewers.size(), similarityThreshold);
            }
            
            return interviewers;
        } catch (Exception e) {
            logger.error("Error finding interviewers by expertise: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<InterviewerProfile> findInterviewersWithFilters(String query, Map<String, String> filters, int limit) {
        try {
            // Get the similarity threshold from configuration
            final double similarityThreshold = matchingConfig.getSimilarityThreshold();
            
            // Check if any vector store entries exist using the JDBC service
            if (!vectorStoreService.existsAny()) {
                logger.warn("No interviewer vector store entries found. Vector store may need to be synced.");
                return new ArrayList<>();
            }
            
            // First get interviewers using the existing method
            List<InterviewerProfile> interviewers = interviewerService.findInterviewersBySimilarityWithFilters(query, filters, limit * 2);
            
            // Then apply similarity threshold filtering
            // For this, we need to get similarity scores for each interviewer
            float[] queryEmbedding = vectorStoreService.generateEmbedding(query);
            String vectorString = vectorStoreService.convertToVectorString(queryEmbedding);
            
            // Get similarity scores for all interviewers
            List<Map<String, Object>> results = vectorStoreService.findSimilarWithScores(vectorString, limit * 3);
            
            // Create a map of interviewer ID to similarity score
            Map<UUID, Double> similarityMap = new HashMap<>();
            for (Map<String, Object> result : results) {
                InterviewerProfile interviewer = (InterviewerProfile) result.get("interviewer");
                Double similarity = (Double) result.get("similarity");
                if (interviewer != null) {
                    similarityMap.put(interviewer.getId(), similarity);
                }
            }
            
            // Filter interviewers by similarity threshold
            List<InterviewerProfile> filteredInterviewers = interviewers.stream()
                    .filter(interviewer -> {
                        Double similarity = similarityMap.get(interviewer.getId());
                        return similarity != null && similarity >= similarityThreshold;
                    })
                    .limit(limit)
                    .collect(Collectors.toList());
            
            // Log the result
            if (filteredInterviewers.isEmpty()) {
                logger.warn("No matching interviewers found for query with filters: {} (similarity threshold: {})", 
                           query, similarityThreshold);
            } else {
                logger.info("Found {} matching interviewers for query with filters (similarity threshold: {})", 
                           filteredInterviewers.size(), similarityThreshold);
            }
            
            return filteredInterviewers;
        } catch (Exception e) {
            logger.error("Error finding interviewers with filters: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public boolean isInterviewerAvailable(UUID interviewerId, String date) {
        InterviewerProfile interviewer = interviewerRepository.findById(interviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Interviewer", "id", interviewerId));
        
        // Check if the interviewer has availability for the specified date
        Map<String, Integer> availability = interviewer.getAvailability();
        if (availability == null || !availability.containsKey(date)) {
            return false;
        }
        
        // Check if there are available slots
        return availability.get(date) > 0;
    }
    
    @Override
    public int getAvailableSlots(UUID interviewerId, String date) {
        InterviewerProfile interviewer = interviewerRepository.findById(interviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Interviewer", "id", interviewerId));
        
        // Get the number of available slots for the specified date
        Map<String, Integer> availability = interviewer.getAvailability();
        if (availability == null || !availability.containsKey(date)) {
            return 0;
        }
        
        return availability.get(date);
    }
    
    @Override
    @Transactional
    public boolean reserveTimeSlot(UUID interviewerId, String date) {
        InterviewerProfile interviewer = interviewerRepository.findById(interviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Interviewer", "id", interviewerId));
        
        // Check if the interviewer has availability for the specified date
        Map<String, Integer> availability = interviewer.getAvailability();
        if (availability == null || !availability.containsKey(date) || availability.get(date) <= 0) {
            return false;
        }
        
        // Decrease the number of available slots
        int availableSlots = availability.get(date);
        availability.put(date, availableSlots - 1);
        
        // Update the interviewer's availability
        interviewer.setAvailability(availability);
        interviewerRepository.save(interviewer);
        
        return true;
    }
    
    @Override
    public String generateMatchSummary(UUID interviewerId, UUID resumeId) {
        InterviewerProfile interviewer = interviewerRepository.findById(interviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Interviewer", "id", interviewerId));
        
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId));
        
        // Extract key information from the resume and interviewer
        String candidateName = resume.getName();
        String candidateFullText = resume.getFullText();
        
        String interviewerName = interviewer.getName();
        String interviewerExpertise = interviewer.getTechnicalExpertise() != null ? 
                String.join(", ", interviewer.getTechnicalExpertise()) : "";
        String interviewerSpecializations = interviewer.getSpecializations() != null ? 
                String.join(", ", interviewer.getSpecializations()) : "";
        int interviewerExperience = interviewer.getExperienceYears();
        
        // Generate a match summary without using AI
        StringBuilder summary = new StringBuilder();
        summary.append("Interviewer ").append(interviewerName)
               .append(" with ").append(interviewerExperience).append(" years of experience ")
               .append("and expertise in ").append(interviewerExpertise)
               .append(" is a good match for candidate ").append(candidateName).append(". ");
        
        summary.append("The interviewer's specializations in ").append(interviewerSpecializations)
               .append(" align well with the candidate's background.");
        
        return summary.toString();
    }
    
    @Override
    public List<InterviewerProfile> findInterviewersWithDualVectorMatch(UUID resumeId, int limit) {
        try {
            // Check if dual vector matching is enabled
            if (!matchingConfig.isDualVectorEnabled()) {
                // If not enabled, fall back to regular matching
                return findInterviewersForCandidate(resumeId, limit);
            }
            
            // Get the dual vector threshold from configuration
            final double dualVectorThreshold = matchingConfig.getDualVectorThreshold();
            
            // Check if any vector store entries exist using the JDBC service
            if (!vectorStoreService.existsAny()) {
                logger.warn("No interviewer vector store entries found. Vector store may need to be synced.");
                return new ArrayList<>();
            }
            
            Resume resume = resumeRepository.findById(resumeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId));
            
            // Extract key information from the resume
            String fullText = resume.getFullText();
            
            // Create a query based on the candidate's resume text
            String query = "Resume content: " + fullText;
            
            // Generate embedding for the query
            float[] queryEmbedding = vectorStoreService.generateEmbedding(query);
            String vectorString = vectorStoreService.convertToVectorString(queryEmbedding);
            
            // Find interviewers with matching expertise and get similarity scores
            // Request more results than needed to allow for filtering
            List<Map<String, Object>> results = vectorStoreService.findSimilarWithScores(vectorString, limit * 2);
            
            // Create a map of interviewer ID to similarity score
            Map<UUID, Double> similarityMap = new HashMap<>();
            for (Map<String, Object> result : results) {
                InterviewerProfile interviewer = (InterviewerProfile) result.get("interviewer");
                Double similarity = (Double) result.get("similarity");
                if (interviewer != null) {
                    // Apply weights to the similarity score
                    double weightedSimilarity = similarity * matchingConfig.getInterviewerWeight();
                    similarityMap.put(interviewer.getId(), weightedSimilarity);
                }
            }
            
            // TODO: Add resume vector store matching and combine scores
            // This would involve getting similarity scores from the resume vector store
            // and combining them with the interviewer vector store scores
            
            // For now, we'll just use the interviewer vector store scores
            // Filter by dual vector threshold and extract interviewer profiles
            List<InterviewerProfile> interviewers = results.stream()
                    .filter(result -> {
                        Double similarity = (Double) result.get("similarity");
                        return similarity != null && similarity >= dualVectorThreshold;
                    })
                    .map(result -> (InterviewerProfile) result.get("interviewer"))
                    .limit(limit)
                    .collect(Collectors.toList());
            
            // Log the result
            if (interviewers.isEmpty()) {
                logger.warn("No matching interviewers found for resume ID: {} (dual vector threshold: {})", 
                           resumeId, dualVectorThreshold);
            } else {
                logger.info("Found {} matching interviewers for resume ID: {} (dual vector threshold: {})", 
                           interviewers.size(), resumeId, dualVectorThreshold);
            }
            
            return interviewers;
        } catch (Exception e) {
            logger.error("Error finding interviewers with dual vector match: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<InterviewerMatchResponse> findInterviewersForEvaluation(UUID evaluationId, int limit, boolean includeNonMatches) {
        try {
            // Get the similarity threshold from configuration
            final double similarityThreshold = matchingConfig.getSimilarityThreshold();
            
            // Check if any vector store entries exist using the JDBC service
            if (!vectorStoreService.existsAny()) {
                logger.warn("No interviewer vector store entries found. Vector store may need to be synced.");
                return new ArrayList<>();
            }
            
            // Get the candidate evaluation
            CandidateEvaluationModel evaluation = evaluationRepository.findById(evaluationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Candidate Evaluation", "id", evaluationId));
            
            // Extract key information from the evaluation
            String executiveSummary = evaluation.getExecutiveSummary();
            List<String> keyStrengths = evaluation.getKeyStrengths();
            List<String> technicalSkills = new ArrayList<>();
            
            // Create a query based on the evaluation
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("Candidate evaluation summary: ").append(executiveSummary).append(". ");
            
            if (keyStrengths != null && !keyStrengths.isEmpty()) {
                queryBuilder.append("Key strengths: ").append(String.join(", ", keyStrengths)).append(". ");
            }
            
            if (technicalSkills != null && !technicalSkills.isEmpty()) {
                queryBuilder.append("Technical skills: ").append(String.join(", ", technicalSkills)).append(". ");
            }
            
            String query = queryBuilder.toString();
            
            // Generate embedding for the query
            float[] queryEmbedding = vectorStoreService.generateEmbedding(query);
            String vectorString = vectorStoreService.convertToVectorString(queryEmbedding);
            
            // Find interviewers with matching expertise and get similarity scores
            // Request more results to ensure we have enough after filtering
            List<Map<String, Object>> results = vectorStoreService.findSimilarWithScores(vectorString, limit * 3);
            
            // Filter by similarity threshold and convert to InterviewerMatchResponse
            List<InterviewerMatchResponse> allInterviewerResponses = results.stream()
                    .filter(result -> (Double)result.get("similarity") >= similarityThreshold)
                    .map(result -> {
                        InterviewerProfile interviewer = (InterviewerProfile) result.get("interviewer");
                        Double similarity = (Double) result.get("similarity");
                        
                        InterviewerMatchResponse response = new InterviewerMatchResponse();
                        response.setInterviewerId(interviewer.getId());
                        response.setName(interviewer.getName());
                        response.setEmail(interviewer.getEmail());
                        response.setExperienceYears(interviewer.getExperienceYears());
                        response.setTechnicalExpertise(interviewer.getTechnicalExpertise());
                        response.setSpecializations(interviewer.getSpecializations());
                        
                        // Convert similarity to a score out of 100
                        int matchScore = (int) Math.round(similarity * 100);
                        response.setMatchScore(matchScore);
                        
                        // Generate a match explanation
                        String matchExplanation = generateMatchExplanationForEvaluation(interviewer, evaluation, similarity);
                        response.setMatchExplanation(matchExplanation);
                        
                        // Determine match status
                        InterviewerMatchResponse.MatchStatus matchStatus = determineMatchStatus(matchExplanation, matchScore);
                        response.setMatchStatus(matchStatus);
                        
                        return response;
                    })
                    .collect(Collectors.toList());
            
            // Separate matches by status
            List<InterviewerMatchResponse> strongMatches = allInterviewerResponses.stream()
                    .filter(interviewer -> interviewer.getMatchStatus() == InterviewerMatchResponse.MatchStatus.STRONG_MATCH)
                    .collect(Collectors.toList());
            
            List<InterviewerMatchResponse> potentialMatches = allInterviewerResponses.stream()
                    .filter(interviewer -> interviewer.getMatchStatus() == InterviewerMatchResponse.MatchStatus.MATCH)
                    .collect(Collectors.toList());
            
            List<InterviewerMatchResponse> considerMatches = allInterviewerResponses.stream()
                    .filter(interviewer -> interviewer.getMatchStatus() == InterviewerMatchResponse.MatchStatus.CONSIDER)
                    .collect(Collectors.toList());
            
            // Create the final result list, prioritizing by match status
            List<InterviewerMatchResponse> result = new ArrayList<>();
            
            // Add strong matches first
            result.addAll(strongMatches.stream().limit(limit).collect(Collectors.toList()));
            
            // If we haven't reached the limit, add potential matches
            if (result.size() < limit) {
                int remainingSlots = limit - result.size();
                result.addAll(potentialMatches.stream().limit(remainingSlots).collect(Collectors.toList()));
            }
            
            // If includeNonMatches is true and we haven't reached the limit, add consider matches
            if (includeNonMatches && result.size() < limit) {
                int remainingSlots = limit - result.size();
                result.addAll(considerMatches.stream().limit(remainingSlots).collect(Collectors.toList()));
            }
            
            // Log the result
            if (result.isEmpty()) {
                logger.warn("No matching interviewers found for evaluation ID: {} (similarity threshold: {})", 
                           evaluationId, similarityThreshold);
            } else {
                logger.info("Found {} matching interviewers for evaluation ID: {} (strong: {}, potential: {}, consider: {})", 
                           result.size(), evaluationId, strongMatches.size(), potentialMatches.size(), considerMatches.size());
            }
            
            return result;
        } catch (Exception e) {
            logger.error("Error finding interviewers for evaluation: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<InterviewerMatchResponse> findInterviewersWithExplanationsForCandidate(UUID resumeId, int limit) {
        try {
            // Get the similarity threshold from configuration
            final double similarityThreshold = matchingConfig.getSimilarityThreshold();
            
            // Check if any vector store entries exist using the JDBC service
            if (!vectorStoreService.existsAny()) {
                logger.warn("No interviewer vector store entries found. Vector store may need to be synced.");
                return new ArrayList<>();
            }
            
            // Get the resume
            Resume resume = resumeRepository.findById(resumeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId));
            
            // Extract key information from the resume
            String fullText = resume.getFullText();
            String candidateName = resume.getName();
            
            // Create a query based on the candidate's resume text
            String query = "Resume content: " + fullText;
            
            // Generate embedding for the query
            float[] queryEmbedding = vectorStoreService.generateEmbedding(query);
            String vectorString = vectorStoreService.convertToVectorString(queryEmbedding);
            
            // Find interviewers with matching expertise and get similarity scores
            // Request more results to ensure we have enough after filtering
            List<Map<String, Object>> results = vectorStoreService.findSimilarWithScores(vectorString, limit * 3);
            
            // Filter by similarity threshold and convert to InterviewerMatchResponse
            List<InterviewerMatchResponse> allInterviewerResponses = results.stream()
                    .filter(result -> (Double)result.get("similarity") >= similarityThreshold)
                    .map(result -> {
                        InterviewerProfile interviewer = (InterviewerProfile) result.get("interviewer");
                        Double similarity = (Double) result.get("similarity");
                        
                        InterviewerMatchResponse response = new InterviewerMatchResponse();
                        response.setInterviewerId(interviewer.getId());
                        response.setName(interviewer.getName());
                        response.setEmail(interviewer.getEmail());
                        response.setExperienceYears(interviewer.getExperienceYears());
                        response.setTechnicalExpertise(interviewer.getTechnicalExpertise());
                        response.setSpecializations(interviewer.getSpecializations());
                        
                        // Convert similarity to a score out of 100
                        int matchScore = (int) Math.round(similarity * 100);
                        response.setMatchScore(matchScore);
                        
                        // Generate a match explanation
                        String matchExplanation = generateMatchExplanationForResume(interviewer, resume, similarity);
                        response.setMatchExplanation(matchExplanation);
                        
                        // Determine match status
                        InterviewerMatchResponse.MatchStatus matchStatus = determineMatchStatus(matchExplanation, matchScore);
                        response.setMatchStatus(matchStatus);
                        
                        return response;
                    })
                    .collect(Collectors.toList());
            
            // Separate matches by status
            List<InterviewerMatchResponse> strongMatches = allInterviewerResponses.stream()
                    .filter(interviewer -> interviewer.getMatchStatus() == InterviewerMatchResponse.MatchStatus.STRONG_MATCH)
                    .collect(Collectors.toList());
            
            List<InterviewerMatchResponse> potentialMatches = allInterviewerResponses.stream()
                    .filter(interviewer -> interviewer.getMatchStatus() == InterviewerMatchResponse.MatchStatus.MATCH)
                    .collect(Collectors.toList());
            
            List<InterviewerMatchResponse> considerMatches = allInterviewerResponses.stream()
                    .filter(interviewer -> interviewer.getMatchStatus() == InterviewerMatchResponse.MatchStatus.CONSIDER)
                    .collect(Collectors.toList());
            
            // Create the final result list, prioritizing by match status
            List<InterviewerMatchResponse> result = new ArrayList<>();
            
            // Add strong matches first
            result.addAll(strongMatches.stream().limit(limit).collect(Collectors.toList()));
            
            // If we haven't reached the limit, add potential matches
            if (result.size() < limit) {
                int remainingSlots = limit - result.size();
                result.addAll(potentialMatches.stream().limit(remainingSlots).collect(Collectors.toList()));
            }
            
            // If we haven't reached the limit, add consider matches
            if (result.size() < limit) {
                int remainingSlots = limit - result.size();
                result.addAll(considerMatches.stream().limit(remainingSlots).collect(Collectors.toList()));
            }
            
            // Log the result
            if (result.isEmpty()) {
                logger.warn("No matching interviewers found for resume ID: {} (similarity threshold: {})", 
                           resumeId, similarityThreshold);
            } else {
                logger.info("Found {} matching interviewers for resume ID: {} (strong: {}, potential: {}, consider: {})", 
                           result.size(), resumeId, strongMatches.size(), potentialMatches.size(), considerMatches.size());
            }
            
            return result;
        } catch (Exception e) {
            logger.error("Error finding interviewers with explanations for candidate: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Generate a match explanation for an interviewer and candidate evaluation using AI.
     * 
     * @param interviewer The interviewer profile
     * @param evaluation The candidate evaluation
     * @param similarity The similarity score
     * @return A match explanation
     */
    private String generateMatchExplanationForEvaluation(
            InterviewerProfile interviewer, 
            CandidateEvaluationModel evaluation,
            double similarity) {
        
        try {
            // Create a prompt with the interviewer and candidate information
            String interviewerName = interviewer.getName();
            int interviewerExperience = interviewer.getExperienceYears();
            List<String> interviewerExpertise = interviewer.getTechnicalExpertise();
            List<String> interviewerSpecializations = interviewer.getSpecializations();
            
            String executiveSummary = evaluation.getExecutiveSummary();
            List<String> keyStrengths = evaluation.getKeyStrengths();
            
            int matchPercentage = (int) Math.round(similarity * 100);
            
            // Build the prompt with all the information
            String prompt = interviewerMatchExplanationPrompt
                    .replace("{{interviewer_name}}", interviewerName)
                    .replace("{{interviewer_experience}}", String.valueOf(interviewerExperience))
                    .replace("{{interviewer_expertise}}", interviewerExpertise != null ? String.join(", ", interviewerExpertise) : "")
                    .replace("{{interviewer_specializations}}", interviewerSpecializations != null ? String.join(", ", interviewerSpecializations) : "")
                    .replace("{{candidate_summary}}", executiveSummary != null ? executiveSummary : "")
                    .replace("{{candidate_strengths}}", keyStrengths != null ? String.join(", ", keyStrengths) : "")
                    .replace("{{match_percentage}}", String.valueOf(matchPercentage));
            
            // Use the ChatModel to generate the explanation
            String explanation = aiRetryTemplate.execute(context -> {
                try {
                    return chatModel.call(prompt);
                } catch (Exception e) {
                    logger.error("Error generating match explanation: {}", e.getMessage(), e);
                    throw new RuntimeException("Failed to generate match explanation", e);
                }
            });
            
            return explanation;
        } catch (Exception e) {
            logger.error("Error generating match explanation: {}", e.getMessage(), e);
            
            // Fallback to a simple explanation if AI fails
            StringBuilder fallbackExplanation = new StringBuilder();
            fallbackExplanation.append("Interviewer ").append(interviewer.getName())
                              .append(" has ").append(interviewer.getExperienceYears())
                              .append(" years of experience");
            
            if (interviewer.getTechnicalExpertise() != null && !interviewer.getTechnicalExpertise().isEmpty()) {
                fallbackExplanation.append(" with expertise in ")
                                  .append(String.join(", ", interviewer.getTechnicalExpertise()));
            }
            
            fallbackExplanation.append(".\n\n");
            fallbackExplanation.append("Match score: ").append((int) Math.round(similarity * 100)).append("%");
            
            return fallbackExplanation.toString();
        }
    }
    
    /**
     * Generate a match explanation for an interviewer and candidate resume using AI.
     * 
     * @param interviewer The interviewer profile
     * @param resume The candidate resume
     * @param similarity The similarity score
     * @return A match explanation
     */
    /**
     * Determine the match status based on the explanation content and score.
     * 
     * @param explanation The match explanation
     * @param matchScore The match score (0-100)
     * @return The match status (STRONG_MATCH, POTENTIAL_MATCH, or CONSIDER)
     */
    private InterviewerMatchResponse.MatchStatus determineMatchStatus(String explanation, int matchScore) {
        // Check for specific recommendation phrases from the prompt
        if (explanation.contains("NOT A MATCH: Consider other options")) {
            return InterviewerMatchResponse.MatchStatus.CONSIDER;
        }
        
        if (explanation.contains("MATCH: Highly recommended for interview")) {
            return InterviewerMatchResponse.MatchStatus.STRONG_MATCH;
        }
        
        if (explanation.contains("MATCH: Recommended for interview")) {
            return InterviewerMatchResponse.MatchStatus.MATCH;
        }
        
        // Fallback to score and keyword analysis if the specific phrases aren't found
        String lowerCaseExplanation = explanation.toLowerCase();
        
        // Lists of keywords for different match levels
        List<String> strongMatchKeywords = Arrays.asList(
            "excellent match", "perfect fit", "highly qualified", "ideal candidate",
            "strong alignment", "exceptional", "outstanding", "highly recommended"
        );
        
        List<String> potentialMatchKeywords = Arrays.asList(
            "good match", "suitable", "qualified", "promising", "potential",
            "adequate", "satisfactory", "recommended"
        );
        
        List<String> negativeKeywords = Arrays.asList(
            "mismatch", "misalignment", "not align", "inappropriate", 
            "consider other options", "significant mismatch", "not recommended",
            "mismatched", "not suitable", "not appropriate", "poor match",
            "not a match"
        );
        
        // Check for strong match keywords
        boolean containsStrongMatchKeywords = strongMatchKeywords.stream()
            .anyMatch(keyword -> lowerCaseExplanation.contains(keyword));
        
        // Check for potential match keywords
        boolean containsPotentialMatchKeywords = potentialMatchKeywords.stream()
            .anyMatch(keyword -> lowerCaseExplanation.contains(keyword));
        
        // Check for negative keywords
        boolean containsNegativeKeywords = negativeKeywords.stream()
            .anyMatch(keyword -> lowerCaseExplanation.contains(keyword));
        
        // Determine match status based on keywords and score
        if (containsStrongMatchKeywords && matchScore >= 85) {
            return InterviewerMatchResponse.MatchStatus.STRONG_MATCH;
        } else if ((containsPotentialMatchKeywords || !containsNegativeKeywords) && matchScore >= 70) {
            return InterviewerMatchResponse.MatchStatus.MATCH;
        } else {
            return InterviewerMatchResponse.MatchStatus.CONSIDER;
        }
    }
    
    private String generateMatchExplanationForResume(
            InterviewerProfile interviewer, 
            Resume resume,
            double similarity) {
        
        try {
            // Create a prompt with the interviewer and candidate information
            String interviewerName = interviewer.getName();
            int interviewerExperience = interviewer.getExperienceYears();
            List<String> interviewerExpertise = interviewer.getTechnicalExpertise();
            List<String> interviewerSpecializations = interviewer.getSpecializations();
            
           
            String candidateFullText = resume.getFullText();
            
         
            
            int matchPercentage = (int) Math.round(similarity * 100);
            
            // Build the prompt with all the information
            String prompt = interviewerMatchExplanationPrompt
                    .replace("{{interviewer_name}}", interviewerName)
                    .replace("{{interviewer_experience}}", String.valueOf(interviewerExperience))
                    .replace("{{interviewer_expertise}}", interviewerExpertise != null ? String.join(", ", interviewerExpertise) : "")
                    .replace("{{interviewer_specializations}}", interviewerSpecializations != null ? String.join(", ", interviewerSpecializations) : "")
                    .replace("{{candidate_summary}}", candidateFullText)
                    .replace("{{candidate_strengths}}", "") // No explicit strengths from resume
                    .replace("{{match_percentage}}", String.valueOf(matchPercentage));
            
            // Use the ChatModel to generate the explanation
            String explanation = aiRetryTemplate.execute(context -> {
                try {
                    return chatModel.call(prompt);
                } catch (Exception e) {
                    logger.error("Error generating match explanation for resume: {}", e.getMessage(), e);
                    throw new RuntimeException("Failed to generate match explanation for resume", e);
                }
            });
            
            return explanation;
        } catch (Exception e) {
            logger.error("Error generating match explanation for resume: {}", e.getMessage(), e);
            
            // Fallback to a simple explanation if AI fails
            StringBuilder fallbackExplanation = new StringBuilder();
            fallbackExplanation.append("Interviewer ").append(interviewer.getName())
                              .append(" has ").append(interviewer.getExperienceYears())
                              .append(" years of experience");
            
            if (interviewer.getTechnicalExpertise() != null && !interviewer.getTechnicalExpertise().isEmpty()) {
                fallbackExplanation.append(" with expertise in ")
                                  .append(String.join(", ", interviewer.getTechnicalExpertise()));
            }
            
            fallbackExplanation.append(".\n\n");
            fallbackExplanation.append("This interviewer's expertise aligns well with the candidate's background.");
            fallbackExplanation.append("\n\nMatch score: ").append((int) Math.round(similarity * 100)).append("%");
            
            return fallbackExplanation.toString();
        }
    }
    
    /**
     * Asynchronously find suitable interviewers for a candidate based on their resume with detailed match explanations.
     * This method uses the resumeProcessingExecutor thread pool for parallel processing.
     */
    @Override
    @Async("resumeProcessingExecutor")
    public CompletableFuture<List<InterviewerMatchResponse>> findInterviewersWithExplanationsForCandidateAsync(UUID resumeId, int limit) {
        logger.info("Starting async interviewer matching for resume ID: {}", resumeId);
        try {
            // Get the similarity threshold from configuration
            final double similarityThreshold = matchingConfig.getSimilarityThreshold();
            
            // Check if any vector store entries exist using the JDBC service
            if (!vectorStoreService.existsAny()) {
                logger.warn("No interviewer vector store entries found. Vector store may need to be synced.");
                return CompletableFuture.completedFuture(new ArrayList<>());
            }
            
            // Get the resume
            Resume resume = resumeRepository.findById(resumeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId));
            
            // Extract key information from the resume
            String fullText = resume.getFullText();
            
            // Create a query based on the candidate's resume text
            String query = "Resume content: " + fullText;
            
            // Generate embedding for the query
            float[] queryEmbedding = vectorStoreService.generateEmbedding(query);
            String vectorString = vectorStoreService.convertToVectorString(queryEmbedding);
            
            // Find interviewers with matching expertise and get similarity scores
            // Request more results to ensure we have enough after filtering
            List<Map<String, Object>> results = vectorStoreService.findSimilarWithScores(vectorString, limit * 3);
            
            // Filter by similarity threshold
            List<Map<String, Object>> filteredResults = results.stream()
                    .filter(result -> (Double)result.get("similarity") >= similarityThreshold)
                    .collect(Collectors.toList());
            
            // Process match explanations in parallel
            List<CompletableFuture<InterviewerMatchResponse>> futureResponses = filteredResults.stream()
                    .map(result -> {
                        InterviewerProfile interviewer = (InterviewerProfile) result.get("interviewer");
                        Double similarity = (Double) result.get("similarity");
                        
                        return CompletableFuture.supplyAsync(() -> {
                            InterviewerMatchResponse response = new InterviewerMatchResponse();
                            response.setInterviewerId(interviewer.getId());
                            response.setName(interviewer.getName());
                            response.setEmail(interviewer.getEmail());
                            response.setExperienceYears(interviewer.getExperienceYears());
                            response.setTechnicalExpertise(interviewer.getTechnicalExpertise());
                            response.setSpecializations(interviewer.getSpecializations());
                            
                            // Convert similarity to a score out of 100
                            int matchScore = (int) Math.round(similarity * 100);
                            response.setMatchScore(matchScore);
                            
                            // Generate a match explanation
                            String matchExplanation = generateMatchExplanationForResume(interviewer, resume, similarity);
                            response.setMatchExplanation(matchExplanation);
                            
                            // Determine match status
                            InterviewerMatchResponse.MatchStatus matchStatus = determineMatchStatus(matchExplanation, matchScore);
                            response.setMatchStatus(matchStatus);
                            
                            return response;
                        }, aiOperationsExecutor());
                    })
                    .collect(Collectors.toList());
            
            // Wait for all futures to complete
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futureResponses.toArray(new CompletableFuture[0]));
            
            // Get all results and process them
            return allFutures.thenApply(v -> {
                List<InterviewerMatchResponse> allInterviewerResponses = futureResponses.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList());
                
                // Separate matches by status
                List<InterviewerMatchResponse> strongMatches = allInterviewerResponses.stream()
                        .filter(interviewer -> interviewer.getMatchStatus() == InterviewerMatchResponse.MatchStatus.STRONG_MATCH)
                        .collect(Collectors.toList());
                
                List<InterviewerMatchResponse> potentialMatches = allInterviewerResponses.stream()
                        .filter(interviewer -> interviewer.getMatchStatus() == InterviewerMatchResponse.MatchStatus.MATCH)
                        .collect(Collectors.toList());
                
                List<InterviewerMatchResponse> considerMatches = allInterviewerResponses.stream()
                        .filter(interviewer -> interviewer.getMatchStatus() == InterviewerMatchResponse.MatchStatus.CONSIDER)
                        .collect(Collectors.toList());
                
                // Create the final result list, prioritizing by match status
                List<InterviewerMatchResponse> result = new ArrayList<>();
                
                // Add strong matches first
                result.addAll(strongMatches.stream().limit(limit).collect(Collectors.toList()));
                
                // If we haven't reached the limit, add potential matches
                if (result.size() < limit) {
                    int remainingSlots = limit - result.size();
                    result.addAll(potentialMatches.stream().limit(remainingSlots).collect(Collectors.toList()));
                }
                
                // If we haven't reached the limit, add consider matches
                if (result.size() < limit) {
                    int remainingSlots = limit - result.size();
                    result.addAll(considerMatches.stream().limit(remainingSlots).collect(Collectors.toList()));
                }
                
                // Log the result
                logger.info("Completed async interviewer matching for resume ID: {}. Found {} strong matches, {} potential matches, and {} consider matches.", 
                           resumeId, strongMatches.size(), potentialMatches.size(), considerMatches.size());
                
                return result;
            });
        } catch (Exception e) {
            logger.error("Error in async interviewer matching for resume: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    
    /**
     * Asynchronously find suitable interviewers for multiple candidates.
     * This method processes each resume in parallel and combines the results.
     */
    @Override
    @Async("resumeProcessingExecutor")
    public CompletableFuture<Map<UUID, List<InterviewerMatchResponse>>> findInterviewersForMultipleCandidatesAsync(
            List<UUID> resumeIds, int limitPerCandidate) {
        
        logger.info("Starting async interviewer matching for multiple resumes: {}", resumeIds);
        
        try {
            // Process each resume in parallel
            List<CompletableFuture<Map.Entry<UUID, List<InterviewerMatchResponse>>>> futures = 
                    resumeIds.stream()
                    .map(resumeId -> findInterviewersWithExplanationsForCandidateAsync(resumeId, limitPerCandidate)
                            .thenApply(responses -> Map.entry(resumeId, responses)))
                    .collect(Collectors.toList());
            
            // Wait for all futures to complete
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));
            
            // Combine results into a map
            return allFutures.thenApply(v -> {
                Map<UUID, List<InterviewerMatchResponse>> result = new HashMap<>();
                
                futures.stream()
                       .map(CompletableFuture::join)
                       .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
                
                logger.info("Completed async interviewer matching for multiple resumes. Processed {} resumes.", resumeIds.size());
                
                return result;
            });
        } catch (Exception e) {
            logger.error("Error in async interviewer matching for multiple resumes: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Get the AI operations executor for parallel processing of AI operations.
     * 
     * @return The AI operations executor
     */
    private Executor aiOperationsExecutor() {
        // In a real implementation, this would be injected via constructor or setter
        // For now, we'll create a simple executor with a few threads
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("AI-Op-");
        executor.initialize();
        return executor;
    }
}
