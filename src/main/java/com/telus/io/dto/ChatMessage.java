package com.telus.io.dto;

import java.time.LocalDateTime;

/**
 * Represents a message in a chat conversation.
 * Used for displaying chat history.
 */
public class ChatMessage {
    
    private String role;  // "user" or "assistant"
    private String content;
    private LocalDateTime timestamp;
    
    // Default constructor
    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
    }
    
    // Constructor with role and content
    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }
    
    // Constructor with all fields
    public ChatMessage(String role, String content, LocalDateTime timestamp) {
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
    }
    
    // Getters and setters
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    // Factory methods for creating user and assistant messages
    public static ChatMessage userMessage(String content) {
        return new ChatMessage("user", content);
    }
    
    public static ChatMessage assistantMessage(String content) {
        return new ChatMessage("assistant", content);
    }
    
    @Override
    public String toString() {
        return "ChatMessage{" +
                "role='" + role + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
