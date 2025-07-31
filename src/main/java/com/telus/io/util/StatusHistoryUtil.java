package com.telus.io.util;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.telus.io.model.CandidateStatus;
import com.telus.io.model.CandidateStatusHistory;
import com.telus.io.repository.CandidateStatusHistoryRepository;



/**
 * Utility class for recording status changes in the history table.
 */
@Component
public class StatusHistoryUtil {
    
    private final CandidateStatusHistoryRepository statusHistoryRepository;
    
    public StatusHistoryUtil(CandidateStatusHistoryRepository statusHistoryRepository) {
        this.statusHistoryRepository = statusHistoryRepository;
    }
    
    /**
     * Record a status change in the history table.
     * 
     * @param resumeId The resume ID
     * @param evaluationId The evaluation ID (can be null)
     * @param previousStatus The previous status
     * @param previousCustomStatus The previous custom status
     * @param newStatus The new status
     * @param newCustomStatus The new custom status
     * @param changedBy The user who made the change
     * @param comments Comments about the change (can be null)
     * @return The saved history record
     */
    public CandidateStatusHistory recordStatusChange(
            UUID resumeId,
            UUID evaluationId,
            CandidateStatus previousStatus,
            String previousCustomStatus,
            CandidateStatus newStatus,
            String newCustomStatus,
            String changedBy,
            String comments) {
        
        CandidateStatusHistory history = new CandidateStatusHistory();
        history.setResumeId(resumeId);
        history.setEvaluationId(evaluationId);
        history.setPreviousStatus(previousStatus);
        history.setPreviousCustomStatus(previousCustomStatus);
        history.setNewStatus(newStatus);
        history.setNewCustomStatus(newCustomStatus);
        history.setChangedBy(changedBy);
        history.setChangedAt(LocalDateTime.now());
        history.setComments(comments);
        
        return statusHistoryRepository.save(history);
    }
}
