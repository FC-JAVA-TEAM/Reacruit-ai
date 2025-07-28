package com.telus.io.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.telus.io.model.Resume;

/**
 * DTO for resume responses to clients.
 * Contains a subset of resume information suitable for API responses.
 */
public class ResumeResponse {
    
    private UUID id;
    private String name;
    private String email;
    private String phoneNumber;
    private LocalDateTime uploadedAt;
    private String fileType;
    private String originalFileName;
    private Integer matchScore;
    private String matchExplanation;
    
    // Default constructor
    public ResumeResponse() {
    }
    
    // Constructor from Resume entity
    public ResumeResponse(Resume resume) {
        this.id = resume.getId();
        this.name = resume.getName();
        this.email = resume.getEmail();
        this.phoneNumber = resume.getPhoneNumber();
        this.uploadedAt = resume.getUploadedAt();
        this.fileType = resume.getFileType();
        this.originalFileName = resume.getOriginalFileName();
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
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
    
    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
    
    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public String getOriginalFileName() {
        return originalFileName;
    }
    
    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }
    
    public Integer getMatchScore() {
        return matchScore;
    }
    
    public void setMatchScore(Integer matchScore) {
        this.matchScore = matchScore;
    }
    
    public String getMatchExplanation() {
        return matchExplanation;
    }
    
    public void setMatchExplanation(String matchExplanation) {
        this.matchExplanation = matchExplanation;
    }
    
    @Override
    public String toString() {
        return "ResumeResponse{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", uploadedAt=" + uploadedAt +
                ", fileType='" + fileType + '\'' +
                ", originalFileName='" + originalFileName + '\'' +
                ", matchScore=" + matchScore +
                ", matchExplanation='" + (matchExplanation != null ? "present" : "null") + '\'' +
                '}';
    }
}
