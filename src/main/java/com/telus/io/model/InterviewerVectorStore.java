package com.telus.io.model;

import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entity representing an interviewer's vector store entry.
 * This is used for semantic search of interviewers based on their expertise.
 */
@Entity
@Table(name = "interviewer_vector_store")
public class InterviewerVectorStore {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "interviewer_id", nullable = false)
    private InterviewerProfile interviewer;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;
    
    @Column(columnDefinition = "vector(1536)", nullable = false)
    private float[] embedding;
    
    // Default constructor
    public InterviewerVectorStore() {
    }
    
    // Constructor with fields
    public InterviewerVectorStore(InterviewerProfile interviewer, String content, 
                                 Map<String, Object> metadata, float[] embedding) {
        this.interviewer = interviewer;
        this.content = content;
        this.metadata = metadata;
        this.embedding = embedding;
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public InterviewerProfile getInterviewer() {
        return interviewer;
    }
    
    public void setInterviewer(InterviewerProfile interviewer) {
        this.interviewer = interviewer;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public float[] getEmbedding() {
        return embedding;
    }
    
    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }
}
