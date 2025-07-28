package com.telus.io.dto;

import java.util.UUID;

/**
 * Represents a chat request from a user.
 * Contains the message and context information.
 */
public class ChatRequest {
    
    private String message;
    private UUID currentResumeId;
    private String currentJobDescription;
    
    // Default constructor
    public ChatRequest() {
    }
    
    // Constructor with fields
    public ChatRequest(String message, UUID currentResumeId, String currentJobDescription) {
        this.message = message;
        this.currentResumeId = currentResumeId;
        this.currentJobDescription = currentJobDescription;
    }
    
    // Getters and setters
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public UUID getCurrentResumeId() {
        return currentResumeId;
    }
    
    public void setCurrentResumeId(UUID currentResumeId) {
        this.currentResumeId = currentResumeId;
    }
    
    public String getCurrentJobDescription() {
        return currentJobDescription;
    }
    
    public void setCurrentJobDescription(String currentJobDescription) {
        this.currentJobDescription = currentJobDescription;
    }
    
    @Override
    public String toString() {
        return "ChatRequest{" +
                "message='" + message + '\'' +
                ", currentResumeId=" + currentResumeId +
                ", currentJobDescription='" + 
                    (currentJobDescription != null ? currentJobDescription.substring(0, Math.min(50, currentJobDescription.length())) + "..." : null) + '\'' +
                '}';
    }
}
