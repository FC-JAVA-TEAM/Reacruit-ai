package com.telus.io.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.telus.io.model.CandidateStatusHistory;


/**
 * Repository for managing candidate status history records.
 */
@Repository
public interface CandidateStatusHistoryRepository extends JpaRepository<CandidateStatusHistory, UUID> {
    
    /**
     * Find status history records by resume ID, ordered by change date (newest first).
     * 
     * @param resumeId The ID of the resume
     * @return List of status history records
     */
    List<CandidateStatusHistory> findByResumeIdOrderByChangedAtDesc(UUID resumeId);
    
    /**
     * Find status history records by evaluation ID, ordered by change date (newest first).
     * 
     * @param evaluationId The ID of the evaluation
     * @return List of status history records
     */
    List<CandidateStatusHistory> findByEvaluationIdOrderByChangedAtDesc(UUID evaluationId);
    
    /**
     * Find status history records by the user who made the change, ordered by change date (newest first).
     * 
     * @param changedBy The ID of the user who made the change
     * @return List of status history records
     */
    List<CandidateStatusHistory> findByChangedByOrderByChangedAtDesc(String changedBy);
}
