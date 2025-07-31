package com.telus.io.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.telus.io.dto.response.ApiResponse;
import com.telus.io.exception.ResourceNotFoundException;
import com.telus.io.model.InterviewerProfile;
import com.telus.io.service.InterviewerProfileService;



/**
 * REST controller for managing interviewer profiles.
 */
@RestController
@RequestMapping("/api/interviewers")
public class InterviewerProfileController {
    
    private final InterviewerProfileService interviewerService;
    
    @Autowired
    public InterviewerProfileController(InterviewerProfileService interviewerService) {
        this.interviewerService = interviewerService;
    }
    
    /**
     * Create a new interviewer profile.
     * 
     * @param interviewer The interviewer profile to create
     * @return The created interviewer profile
     */
    @PostMapping
    public ResponseEntity<InterviewerProfile> createInterviewer(@RequestBody InterviewerProfile interviewer) {
        InterviewerProfile createdInterviewer = interviewerService.createInterviewer(interviewer);
        return new ResponseEntity<>(createdInterviewer, HttpStatus.CREATED);
    }
    
    /**
     * Update an existing interviewer profile.
     * 
     * @param id The ID of the interviewer profile to update
     * @param interviewer The updated interviewer profile
     * @return The updated interviewer profile
     */
    @PutMapping("/{id}")
    public ResponseEntity<InterviewerProfile> updateInterviewer(
            @PathVariable UUID id, @RequestBody InterviewerProfile interviewer) {
        InterviewerProfile updatedInterviewer = interviewerService.updateInterviewer(id, interviewer);
        return ResponseEntity.ok(updatedInterviewer);
    }
    
    /**
     * Get an interviewer profile by ID.
     * 
     * @param id The ID of the interviewer profile to get
     * @return The interviewer profile
     */
    @GetMapping("/{id}")
    public ResponseEntity<InterviewerProfile> getInterviewerById(@PathVariable UUID id) {
        return interviewerService.getInterviewerById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Interviewer", "id", id));
    }
    
    /**
     * Get all interviewer profiles.
     * 
     * @return A list of all interviewer profiles
     */
    @GetMapping
    public ResponseEntity<List<InterviewerProfile>> getAllInterviewers() {
        List<InterviewerProfile> interviewers = interviewerService.getAllInterviewers();
        return ResponseEntity.ok(interviewers);
    }
    
    /**
     * Delete an interviewer profile by ID.
     * 
     * @param id The ID of the interviewer profile to delete
     * @return A success message
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteInterviewer(@PathVariable UUID id) {
        interviewerService.deleteInterviewer(id);
        return ResponseEntity.ok(new ApiResponse(true, "Interviewer deleted successfully"));
    }
    
    /**
     * Update an interviewer's availability.
     * 
     * @param id The ID of the interviewer profile to update
     * @param availability The updated availability map (date string -> available slots)
     * @return The updated interviewer profile
     */
    @PutMapping("/{id}/availability")
    public ResponseEntity<InterviewerProfile> updateInterviewerAvailability(
            @PathVariable UUID id, @RequestBody Map<String, Integer> availability) {
        InterviewerProfile updatedInterviewer = interviewerService.updateInterviewerAvailability(id, availability);
        return ResponseEntity.ok(updatedInterviewer);
    }
    
    /**
     * Find interviewers by technical expertise.
     * 
     * @param expertise The technical expertise to search for
     * @return A list of interviewer profiles with the specified expertise
     */
    @GetMapping("/expertise")
    public ResponseEntity<List<InterviewerProfile>> findInterviewersByExpertise(
            @RequestParam String expertise) {
        List<InterviewerProfile> interviewers = interviewerService.findInterviewersByExpertise(expertise);
        return ResponseEntity.ok(interviewers);
    }
    
    /**
     * Find interviewers by specialization.
     * 
     * @param specialization The specialization to search for
     * @return A list of interviewer profiles with the specified specialization
     */
    @GetMapping("/specialization")
    public ResponseEntity<List<InterviewerProfile>> findInterviewersBySpecialization(
            @RequestParam String specialization) {
        List<InterviewerProfile> interviewers = interviewerService.findInterviewersBySpecialization(specialization);
        return ResponseEntity.ok(interviewers);
    }
    
    /**
     * Find interviewers by tier.
     * 
     * @param tier The interviewer tier to search for
     * @return A list of interviewer profiles with the specified tier
     */
    @GetMapping("/tier")
    public ResponseEntity<List<InterviewerProfile>> findInterviewersByTier(
            @RequestParam int tier) {
        List<InterviewerProfile> interviewers = interviewerService.findInterviewersByTier(tier);
        return ResponseEntity.ok(interviewers);
    }
    
    /**
     * Find interviewers by minimum experience years.
     * 
     * @param years The minimum experience years to search for
     * @return A list of interviewer profiles with at least the specified years of experience
     */
    @GetMapping("/experience")
    public ResponseEntity<List<InterviewerProfile>> findInterviewersByMinimumExperience(
            @RequestParam int years) {
        List<InterviewerProfile> interviewers = interviewerService.findInterviewersByMinimumExperience(years);
        return ResponseEntity.ok(interviewers);
    }
    
    /**
     * Find interviewers by semantic search.
     * 
     * @param query The search query
     * @param limit The maximum number of results to return (default: 10)
     * @return A list of interviewer profiles ordered by relevance to the query
     */
    @GetMapping("/search")
    public ResponseEntity<List<InterviewerProfile>> findInterviewersBySimilarity(
            @RequestParam String query, @RequestParam(defaultValue = "10") int limit) {
        List<InterviewerProfile> interviewers = interviewerService.findInterviewersBySimilarity(query, limit);
        return ResponseEntity.ok(interviewers);
    }
}
