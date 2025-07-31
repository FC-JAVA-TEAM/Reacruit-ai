package com.telus.io.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for interviewer matches with scores and explanations.
 */
public class InterviewerMatchResponse {
    
    /**
     * Enum representing the match status between an interviewer and a candidate.
     * STRONG_MATCH: Highly qualified and well-aligned with the interviewer's expertise
     * POTENTIAL_MATCH: Shows promise but may need additional consideration
     * CONSIDER: Not an ideal match but might still be worth considering
     */
    public enum MatchStatus {
        STRONG_MATCH,
        MATCH,
        CONSIDER
    }
    
    private UUID interviewerId;
    private String name;
    private String email;
    private int experienceYears;
    private List<String> technicalExpertise;
    private List<String> specializations;
    private int matchScore;  // 0-100 score
    private String matchExplanation;  // AI-generated explanation
    private MatchStatus matchStatus;  // Whether this is a good match or not
    
    public UUID getInterviewerId() {
        return interviewerId;
    }
    
    public void setInterviewerId(UUID interviewerId) {
        this.interviewerId = interviewerId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public int getExperienceYears() {
        return experienceYears;
    }
    
    public void setExperienceYears(int experienceYears) {
        this.experienceYears = experienceYears;
    }
    
    public List<String> getTechnicalExpertise() {
        return technicalExpertise;
    }
    
    public void setTechnicalExpertise(List<String> technicalExpertise) {
        this.technicalExpertise = technicalExpertise;
    }
    
    public List<String> getSpecializations() {
        return specializations;
    }
    
    public void setSpecializations(List<String> specializations) {
        this.specializations = specializations;
    }
    
    public int getMatchScore() {
        return matchScore;
    }
    
    public void setMatchScore(int matchScore) {
        this.matchScore = matchScore;
    }
    
    public String getMatchExplanation() {
        return matchExplanation;
    }
    
    public void setMatchExplanation(String matchExplanation) {
        this.matchExplanation = matchExplanation;
    }
    
    public MatchStatus getMatchStatus() {
        return matchStatus;
    }
    
    public void setMatchStatus(MatchStatus matchStatus) {
        this.matchStatus = matchStatus;
    }
}
