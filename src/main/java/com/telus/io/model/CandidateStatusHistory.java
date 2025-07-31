package com.telus.io.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity class for storing candidate status history.
 * This provides an audit trail of status changes.
 */
@Entity
@Table(name = "candidate_status_history")
public class CandidateStatusHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "resume_id", nullable = false)
    private UUID resumeId;
    
    @Column(name = "evaluation_id")
    private UUID evaluationId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    private CandidateStatus previousStatus;
    
    @Column(name = "previous_custom_status")
    private String previousCustomStatus;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private CandidateStatus newStatus;
    
    @Column(name = "new_custom_status")
    private String newCustomStatus;
    
    @Column(name = "changed_by", nullable = false)
    private String changedBy;
    
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
    
    @Column(name = "comments")
    private String comments;
    
    // Default constructor
    public CandidateStatusHistory() {
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getResumeId() {
        return resumeId;
    }
    
    public void setResumeId(UUID resumeId) {
        this.resumeId = resumeId;
    }
    
    public UUID getEvaluationId() {
        return evaluationId;
    }
    
    public void setEvaluationId(UUID evaluationId) {
        this.evaluationId = evaluationId;
    }
    
    public CandidateStatus getPreviousStatus() {
        return previousStatus;
    }
    
    public void setPreviousStatus(CandidateStatus previousStatus) {
        this.previousStatus = previousStatus;
    }
    
    public String getPreviousCustomStatus() {
        return previousCustomStatus;
    }
    
    public void setPreviousCustomStatus(String previousCustomStatus) {
        this.previousCustomStatus = previousCustomStatus;
    }
    
    public CandidateStatus getNewStatus() {
        return newStatus;
    }
    
    public void setNewStatus(CandidateStatus newStatus) {
        this.newStatus = newStatus;
    }
    
    public String getNewCustomStatus() {
        return newCustomStatus;
    }
    
    public void setNewCustomStatus(String newCustomStatus) {
        this.newCustomStatus = newCustomStatus;
    }
    
    public String getChangedBy() {
        return changedBy;
    }
    
    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }
    
    public LocalDateTime getChangedAt() {
        return changedAt;
    }
    
    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
    
    public String getComments() {
        return comments;
    }
    
    public void setComments(String comments) {
        this.comments = comments;
    }
    
    @Override
    public String toString() {
        return "CandidateStatusHistory{" +
               "id=" + id +
               ", resumeId=" + resumeId +
               ", evaluationId=" + evaluationId +
               ", previousStatus=" + previousStatus +
               ", previousCustomStatus='" + previousCustomStatus + '\'' +
               ", newStatus=" + newStatus +
               ", newCustomStatus='" + newCustomStatus + '\'' +
               ", changedBy='" + changedBy + '\'' +
               ", changedAt=" + changedAt +
               ", comments='" + comments + '\'' +
               '}';
    }
}
