package com.telus.io.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a response from the chat system.
 * Contains the message and any additional data.
 */
public class ChatResponse {
    
    private String message;
    private Map<String, Object> additionalData;
    
    // Default constructor
    public ChatResponse() {
        this.additionalData = new HashMap<>();
    }
    
    // Constructor with message
    public ChatResponse(String message) {
        this.message = message;
        this.additionalData = new HashMap<>();
    }
    
    // Constructor with message and additional data
    public ChatResponse(String message, Map<String, Object> additionalData) {
        this.message = message;
        this.additionalData = additionalData != null ? additionalData : new HashMap<>();
    }
    
    // Getters and setters
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }
    
    public void setAdditionalData(Map<String, Object> additionalData) {
        this.additionalData = additionalData != null ? additionalData : new HashMap<>();
    }
    
    // Helper method to add additional data
    public void addData(String key, Object value) {
        if (this.additionalData == null) {
            this.additionalData = new HashMap<>();
        }
        this.additionalData.put(key, value);
    }
    
    @Override
    public String toString() {
        return "ChatResponse{" +
                "message='" + message + '\'' +
                ", additionalData=" + additionalData +
                '}';
    }
}
