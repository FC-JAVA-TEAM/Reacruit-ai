package com.telus.io.dto;

import java.util.List;

/**
 * Data Transfer Object for structured job descriptions.
 */
public class JobDescriptionDTO {
    private String title;
    private String location;
    private String company;
    private String overview;
    private String summary;
    private List<String> responsibilities;
    private List<String> requiredQualifications;
    private List<String> preferredQualifications;
    private List<String> benefits;
    private List<String> technicalSkills;
    
    // Default constructor for Jackson
    public JobDescriptionDTO() {
    }
    
    // Getters and setters
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getCompany() {
        return company;
    }
    
    public void setCompany(String company) {
        this.company = company;
    }
    
    public String getOverview() {
        return overview;
    }
    
    public void setOverview(String overview) {
        this.overview = overview;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public List<String> getResponsibilities() {
        return responsibilities;
    }
    
    public void setResponsibilities(List<String> responsibilities) {
        this.responsibilities = responsibilities;
    }
    
    public List<String> getRequiredQualifications() {
        return requiredQualifications;
    }
    
    public void setRequiredQualifications(List<String> requiredQualifications) {
        this.requiredQualifications = requiredQualifications;
    }
    
    public List<String> getPreferredQualifications() {
        return preferredQualifications;
    }
    
    public void setPreferredQualifications(List<String> preferredQualifications) {
        this.preferredQualifications = preferredQualifications;
    }
    
    public List<String> getBenefits() {
        return benefits;
    }
    
    public void setBenefits(List<String> benefits) {
        this.benefits = benefits;
    }
    
    public List<String> getTechnicalSkills() {
        return technicalSkills;
    }
    
    public void setTechnicalSkills(List<String> technicalSkills) {
        this.technicalSkills = technicalSkills;
    }
}
