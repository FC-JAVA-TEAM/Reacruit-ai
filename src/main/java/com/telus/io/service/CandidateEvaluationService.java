package com.telus.io.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.telus.io.dto.request.LockResumeRequest;
import com.telus.io.model.CandidateEvaluationModel;



/**
 * Service interface for managing candidate evaluations.
 */
public interface CandidateEvaluationService {
    

    /**
     * Process a lock/unlock request with optional evaluation data.
     * This method handles all the business logic for locking/unlocking resumes,
     * including dirty checking and validation.
     * 
     * @param request The lock/unlock request
     * @return The processed evaluation
     */
    CandidateEvaluationModel processLockRequest(LockResumeRequest request);

	Optional<CandidateEvaluationModel> findByResumeId(UUID resumeId);

	List<CandidateEvaluationModel> findByLocked(boolean locked);
    
    
}
