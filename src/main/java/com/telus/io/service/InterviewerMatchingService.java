package com.telus.io.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.telus.io.dto.response.InterviewerMatchResponse;
import com.telus.io.model.InterviewerProfile;
import com.telus.io.model.ResumeMatch;



/**
 * Service interface for matching interviewers with candidates.
 */
public interface InterviewerMatchingService {
    
    /**
     * Find suitable interviewers for a candidate based on their resume.
     * 
     * @param resumeId The ID of the candidate's resume
     * @param limit The maximum number of interviewers to return
     * @return A list of interviewer profiles ordered by relevance
     */
    List<InterviewerProfile> findInterviewersForCandidate(UUID resumeId, int limit);
    
    /**
     * Find suitable interviewers for a candidate based on their resume match.
     * 
     * @param resumeMatch The resume match containing candidate skills and job requirements
     * @param limit The maximum number of interviewers to return
     * @return A list of interviewer profiles ordered by relevance
     */
    List<InterviewerProfile> findInterviewersForMatch(ResumeMatch resumeMatch, int limit);
    
    /**
     * Find suitable interviewers based on a text query describing the required expertise.
     * 
     * @param query The query describing the required expertise
     * @param limit The maximum number of interviewers to return
     * @return A list of interviewer profiles ordered by relevance
     */
    List<InterviewerProfile> findInterviewersByExpertise(String query, int limit);
    
    /**
     * Find suitable interviewers based on a text query and additional filters.
     * 
     * @param query The query describing the required expertise
     * @param filters Additional filters to apply (e.g., minimum experience, tier)
     * @param limit The maximum number of interviewers to return
     * @return A list of interviewer profiles ordered by relevance
     */
    List<InterviewerProfile> findInterviewersWithFilters(String query, Map<String, String> filters, int limit);
    
    /**
     * Check if an interviewer is available on a specific date.
     * 
     * @param interviewerId The ID of the interviewer
     * @param date The date to check (in format YYYY-MM-DD)
     * @return true if the interviewer has available slots on the specified date
     */
    boolean isInterviewerAvailable(UUID interviewerId, String date);
    
    /**
     * Get the available time slots for an interviewer on a specific date.
     * 
     * @param interviewerId The ID of the interviewer
     * @param date The date to check (in format YYYY-MM-DD)
     * @return The number of available slots, or 0 if none available
     */
    int getAvailableSlots(UUID interviewerId, String date);
    
    /**
     * Reserve a time slot for an interviewer on a specific date.
     * 
     * @param interviewerId The ID of the interviewer
     * @param date The date to reserve (in format YYYY-MM-DD)
     * @return true if the reservation was successful
     */
    boolean reserveTimeSlot(UUID interviewerId, String date);
    
/**
 * Generate a summary of why an interviewer is a good match for a candidate.
 * 
 * @param interviewerId The ID of the interviewer
 * @param resumeId The ID of the candidate's resume
 * @return A summary explaining the match
 */
String generateMatchSummary(UUID interviewerId, UUID resumeId);

/**
 * Find suitable interviewers for a candidate using dual vector matching.
 * This method uses both resume and interviewer vector stores for more accurate matching.
 * 
 * @param resumeId The ID of the candidate's resume
 * @param limit The maximum number of interviewers to return
 * @return A list of interviewer profiles ordered by combined similarity score
 */
List<InterviewerProfile> findInterviewersWithDualVectorMatch(UUID resumeId, int limit);

/**
 * Find suitable interviewers for a candidate evaluation.
 * 
 * @param evaluationId The ID of the candidate evaluation
 * @param limit The maximum number of interviewers to return
 * @param includeNonMatches Whether to include non-matching interviewers in the results
 * @return A list of interviewer match responses ordered by relevance
 */
List<InterviewerMatchResponse> findInterviewersForEvaluation(UUID evaluationId, int limit, boolean includeNonMatches);

/**
 * Find suitable interviewers for a candidate based on their resume with detailed match explanations.
 * 
 * @param resumeId The ID of the candidate's resume
 * @param limit The maximum number of interviewers to return
 * @return A list of interviewer match responses ordered by relevance, including match explanations
 */
List<InterviewerMatchResponse> findInterviewersWithExplanationsForCandidate(UUID resumeId, int limit);

/**
 * Asynchronously find suitable interviewers for a candidate based on their resume with detailed match explanations.
 * 
 * @param resumeId The ID of the candidate's resume
 * @param limit The maximum number of interviewers to return
 * @return A CompletableFuture containing a list of interviewer match responses ordered by relevance
 */
CompletableFuture<List<InterviewerMatchResponse>> findInterviewersWithExplanationsForCandidateAsync(UUID resumeId, int limit);



/**
 * Asynchronously find suitable interviewers for multiple candidates.
 * 
 * @param resumeIds List of resume IDs to process
 * @param limitPerCandidate The maximum number of interviewers to return per candidate
 * @return A CompletableFuture containing a map of resume IDs to lists of interviewer match responses
 */
CompletableFuture<Map<UUID, List<InterviewerMatchResponse>>> findInterviewersForMultipleCandidatesAsync(
        List<UUID> resumeIds, int limitPerCandidate);
}
