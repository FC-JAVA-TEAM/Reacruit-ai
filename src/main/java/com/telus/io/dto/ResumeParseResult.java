package com.telus.io.dto;

/**
 * Represents the result of parsing a resume file.
 * Contains extracted information and the full text content.
 */
public class ResumeParseResult {
    
    private String name;
    private String email;
    private String phoneNumber;
    private String fullText;
    private String fileType;
    
    // Default constructor
    public ResumeParseResult() {
    }
    
    // Constructor with fields
    public ResumeParseResult(String name, String email, String phoneNumber, String fullText, String fileType) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.fullText = fullText;
        this.fileType = fileType;
    }
    
    // Getters and setters
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
    
    public String getFullText() {
        return fullText;
    }
    
    public void setFullText(String fullText) {
        this.fullText = fullText;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    @Override
    public String toString() {
        return "ResumeParseResult{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", fileType='" + fileType + '\'' +
                ", fullTextLength=" + (fullText != null ? fullText.length() : 0) +
                '}';
    }
}
