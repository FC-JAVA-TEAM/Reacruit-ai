package com.telus.io.dto;

import java.util.UUID;

/**
 * Represents a chat request from a user.
 * Contains the message and context information.
 */
public class ChatRequest_user {
    
    private String message;
    private UUID currentResumeId;
    
    // Default constructor
    public ChatRequest_user() {
    }
    
    // Constructor with fields
    public ChatRequest_user(String message, UUID currentResumeId, String currentJobDescription) {
        this.message = message;
        this.currentResumeId = currentResumeId;
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
    
   
    
    @Override
    public String toString() {
        return "ChatRequest{" +
                "message='" + message + '\'' +
                ", currentResumeId=" + currentResumeId +
                ", currentJobDescription='" + 
                '}';
    }
}
