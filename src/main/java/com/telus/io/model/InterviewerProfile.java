package com.telus.io.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Entity representing an interviewer profile.
 */
@Entity
@Table(name = "interviewer_profiles")
public class InterviewerProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String email;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Column(name = "experience_years", nullable = false)
    private int experienceYears;
    
    @Column(name = "interviewer_tier", nullable = false)
    private int interviewerTier;
    
    @Column(name = "max_interviews_per_day", nullable = false)
    private int maxInterviewsPerDay = 3; // Default value
    
    @Column(name = "technical_expertise", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> technicalExpertise;
    
    @Column(name = "specializations", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> specializations;
    
    @Column(name = "availability", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Integer> availability; // Date string -> available slots
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Default constructor
    public InterviewerProfile() {
    }
    
    // Constructor with fields
    public InterviewerProfile(String name, String email, String phoneNumber, int experienceYears, 
                             int interviewerTier, int maxInterviewsPerDay, 
                             List<String> technicalExpertise, List<String> specializations) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.experienceYears = experienceYears;
        this.interviewerTier = interviewerTier;
        this.maxInterviewsPerDay = maxInterviewsPerDay;
        this.technicalExpertise = technicalExpertise;
        this.specializations = specializations;
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
    
    public int getExperienceYears() {
        return experienceYears;
    }
    
    public void setExperienceYears(int experienceYears) {
        this.experienceYears = experienceYears;
    }
    
    public int getInterviewerTier() {
        return interviewerTier;
    }
    
    public void setInterviewerTier(int interviewerTier) {
        this.interviewerTier = interviewerTier;
    }
    
    public int getMaxInterviewsPerDay() {
        return maxInterviewsPerDay;
    }
    
    public void setMaxInterviewsPerDay(int maxInterviewsPerDay) {
        this.maxInterviewsPerDay = maxInterviewsPerDay;
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
    
    public Map<String, Integer> getAvailability() {
        return availability;
    }
    
    public void setAvailability(Map<String, Integer> availability) {
        this.availability = availability;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public String toString() {
        return "InterviewerProfile{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", experienceYears=" + experienceYears +
                ", interviewerTier=" + interviewerTier +
                ", maxInterviewsPerDay=" + maxInterviewsPerDay +
                ", technicalExpertise=" + technicalExpertise +
                ", specializations=" + specializations +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
