package com.telus.io.dto.request;

import java.util.List;
import java.util.UUID;

/**
 * DTO for resume lock/unlock operations.
 */
public class LockResumeRequest {
    
    private UUID resumeId;
    private String managerId;
    private boolean locked;
    
    // Optional evaluation data
    private String name;
    private String email;
    private String phoneNumber;
    private int score;
    private String executiveSummary;
    private List<String> keyStrengths;
    private List<String> improvementAreas;
    private String technicalSkills;
    private String experience;
    private String education;
    private String softSkills;
    private String achievements;
    private String recommendationType;
    private String recommendationReason;
    private String status;
    
    // Getters and setters
    
    public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public UUID getResumeId() {
        return resumeId;
    }
    
    public void setResumeId(UUID resumeId) {
        this.resumeId = resumeId;
    }
    
    public String getManagerId() {
        return managerId;
    }
    
    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }
    
    public boolean isLocked() {
        return locked;
    }
    
    public void setLocked(boolean locked) {
        this.locked = locked;
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
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public int getScore() {
        return score;
    }
    
    public void setScore(int score) {
        this.score = score;
    }
    
    public String getExecutiveSummary() {
        return executiveSummary;
    }
    
    public void setExecutiveSummary(String executiveSummary) {
        this.executiveSummary = executiveSummary;
    }
    
    public List<String> getKeyStrengths() {
        return keyStrengths;
    }
    
    public void setKeyStrengths(List<String> keyStrengths) {
        this.keyStrengths = keyStrengths;
    }
    
    public List<String> getImprovementAreas() {
        return improvementAreas;
    }
    
    public void setImprovementAreas(List<String> improvementAreas) {
        this.improvementAreas = improvementAreas;
    }
    
    public String getTechnicalSkills() {
        return technicalSkills;
    }
    
    public void setTechnicalSkills(String technicalSkills) {
        this.technicalSkills = technicalSkills;
    }
    
    public String getExperience() {
        return experience;
    }
    
    public void setExperience(String experience) {
        this.experience = experience;
    }
    
    public String getEducation() {
        return education;
    }
    
    public void setEducation(String education) {
        this.education = education;
    }
    
    public String getSoftSkills() {
        return softSkills;
    }
    
    public void setSoftSkills(String softSkills) {
        this.softSkills = softSkills;
    }
    
    public String getAchievements() {
        return achievements;
    }
    
    public void setAchievements(String achievements) {
        this.achievements = achievements;
    }
    
    public String getRecommendationType() {
        return recommendationType;
    }
    
    public void setRecommendationType(String recommendationType) {
        this.recommendationType = recommendationType;
    }
    
    public String getRecommendationReason() {
        return recommendationReason;
    }
    
    public void setRecommendationReason(String recommendationReason) {
        this.recommendationReason = recommendationReason;
    }
    
    /**
     * Checks if this request contains evaluation data.
     * 
     * @return true if the request contains evaluation data, false otherwise
     */
    public boolean hasEvaluationData() {
        return executiveSummary != null;
    }
    
}
