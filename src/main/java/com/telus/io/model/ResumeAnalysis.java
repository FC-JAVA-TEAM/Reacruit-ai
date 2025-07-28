package com.telus.io.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Structured representation of the AI's resume analysis.
 * This class maps directly to the AI's response format.
 */
@JsonPropertyOrder({
    "executiveSummary", 
    "overallScore", 
    "keyStrengths", 
    "improvementAreas", 
    "categoryScores", 
    "skillExplanations",
    "recommendation"
})
public class ResumeAnalysis {
    private String executiveSummary;
    private Integer overallScore;
    private List<KeyStrength> keyStrengths;
    private List<ImprovementArea> improvementAreas;
    private CategoryScores categoryScores;
    private Map<String, String> skillExplanations = Map.of(); // Initialize as empty map
    private Recommendation recommendation;
    
    /**
     * Represents a key strength identified in the resume.
     */
    public static class KeyStrength {
        private String strength;
        private String evidence;
        
        // Getters and setters
        public String getStrength() { return strength; }
        public void setStrength(String strength) { this.strength = strength; }
        public String getEvidence() { return evidence; }
        public void setEvidence(String evidence) { this.evidence = evidence; }
    }
    
    /**
     * Represents an area for improvement identified in the resume.
     */
    public static class ImprovementArea {
        private String gap;
        private String suggestion;
        
        // Getters and setters
        public String getGap() { return gap; }
        public void setGap(String gap) { this.gap = gap; }
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    }
    
    /**
     * Represents the scores for each category in the resume analysis.
     */
    public static class CategoryScores {
        private Integer technicalSkills;
        private Integer experience;
        private Integer education;
        private Integer softSkills;
        private Integer achievements;
        
        // Getters and setters
        public Integer getTechnicalSkills() { return technicalSkills; }
        public void setTechnicalSkills(Integer technicalSkills) { this.technicalSkills = technicalSkills; }
        public Integer getExperience() { return experience; }
        public void setExperience(Integer experience) { this.experience = experience; }
        public Integer getEducation() { return education; }
        public void setEducation(Integer education) { this.education = education; }
        public Integer getSoftSkills() { return softSkills; }
        public void setSoftSkills(Integer softSkills) { this.softSkills = softSkills; }
        public Integer getAchievements() { return achievements; }
        public void setAchievements(Integer achievements) { this.achievements = achievements; }
    }
    
    /**
     * Represents the final recommendation for the candidate.
     */
    public static class Recommendation {
        private String type;
        private String reason;
        
        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
    
    // Getters and setters for main class
    public String getExecutiveSummary() { return executiveSummary; }
    public void setExecutiveSummary(String executiveSummary) { this.executiveSummary = executiveSummary; }
    public Integer getOverallScore() { return overallScore; }
    public void setOverallScore(Integer overallScore) { this.overallScore = overallScore; }
    public List<KeyStrength> getKeyStrengths() { return keyStrengths; }
    public void setKeyStrengths(List<KeyStrength> keyStrengths) { this.keyStrengths = keyStrengths; }
    public List<ImprovementArea> getImprovementAreas() { return improvementAreas; }
    public void setImprovementAreas(List<ImprovementArea> improvementAreas) { this.improvementAreas = improvementAreas; }
    public CategoryScores getCategoryScores() { return categoryScores; }
    public void setCategoryScores(CategoryScores categoryScores) { this.categoryScores = categoryScores; }
    public Map<String, String> getSkillExplanations() { return skillExplanations; }
    public void setSkillExplanations(Map<String, String> skillExplanations) { this.skillExplanations = skillExplanations; }
    public Recommendation getRecommendation() { return recommendation; }
    public void setRecommendation(Recommendation recommendation) { this.recommendation = recommendation; }
}
