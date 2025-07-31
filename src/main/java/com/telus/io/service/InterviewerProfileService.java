package com.telus.io.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.telus.io.model.InterviewerProfile;


/**
 * Service for managing interviewer profiles.
 */
public interface InterviewerProfileService {
    
    /**
     * Create a new interviewer profile.
     * 
     * @param interviewer The interviewer profile to create
     * @return The created interviewer profile
     */
    InterviewerProfile createInterviewer(InterviewerProfile interviewer);
    
    /**
     * Update an existing interviewer profile.
     * 
     * @param id The ID of the interviewer profile to update
     * @param interviewer The updated interviewer profile
     * @return The updated interviewer profile
     */
    InterviewerProfile updateInterviewer(UUID id, InterviewerProfile interviewer);
    
    /**
     * Get an interviewer profile by ID.
     * 
     * @param id The ID of the interviewer profile to get
     * @return The interviewer profile, if found
     */
    Optional<InterviewerProfile> getInterviewerById(UUID id);
    
    /**
     * Get an interviewer profile by email.
     * 
     * @param email The email of the interviewer profile to get
     * @return The interviewer profile, if found
     */
    Optional<InterviewerProfile> getInterviewerByEmail(String email);
    
    /**
     * Get all interviewer profiles.
     * 
     * @return A list of all interviewer profiles
     */
    List<InterviewerProfile> getAllInterviewers();
    
    /**
     * Delete an interviewer profile by ID.
     * 
     * @param id The ID of the interviewer profile to delete
     */
    void deleteInterviewer(UUID id);
    
    /**
     * Find interviewers by technical expertise.
     * 
     * @param expertise The technical expertise to search for
     * @return A list of interviewer profiles with the specified expertise
     */
    List<InterviewerProfile> findInterviewersByExpertise(String expertise);
    
    /**
     * Find interviewers by specialization.
     * 
     * @param specialization The specialization to search for
     * @return A list of interviewer profiles with the specified specialization
     */
    List<InterviewerProfile> findInterviewersBySpecialization(String specialization);
    
    /**
     * Find interviewers by tier.
     * 
     * @param tier The interviewer tier to search for
     * @return A list of interviewer profiles with the specified tier
     */
    List<InterviewerProfile> findInterviewersByTier(int tier);
    
    /**
     * Find interviewers by minimum experience years.
     * 
     * @param years The minimum experience years to search for
     * @return A list of interviewer profiles with at least the specified years of experience
     */
    List<InterviewerProfile> findInterviewersByMinimumExperience(int years);
    
    /**
     * Update an interviewer's availability.
     * 
     * @param id The ID of the interviewer profile to update
     * @param availability The updated availability map (date string -> available slots)
     * @return The updated interviewer profile
     */
    InterviewerProfile updateInterviewerAvailability(UUID id, Map<String, Integer> availability);
    
    /**
     * Find interviewers by semantic search using the vector store.
     * 
     * @param query The search query
     * @param limit The maximum number of results to return
     * @return A list of interviewer profiles ordered by relevance to the query
     */
    List<InterviewerProfile> findInterviewersBySimilarity(String query, int limit);
    
    /**
     * Find interviewers by semantic search with filters.
     * 
     * @param query The search query
     * @param filters A map of metadata filters (key -> value)
     * @param limit The maximum number of results to return
     * @return A list of interviewer profiles ordered by relevance to the query and filtered by metadata
     */
    List<InterviewerProfile> findInterviewersBySimilarityWithFilters(
            String query, Map<String, String> filters, int limit);
}
