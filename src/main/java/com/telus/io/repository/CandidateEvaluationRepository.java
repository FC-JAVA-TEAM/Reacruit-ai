package com.telus.io.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.telus.io.model.CandidateEvaluationModel;


/**
 * Repository for managing CandidateEvaluationDTO entities.
 */
@Repository
public interface CandidateEvaluationRepository extends JpaRepository<CandidateEvaluationModel, UUID> {
    
    /**
     * Find all candidate evaluations by their locked status.
     * 
     * @param locked The locked status to filter by
     * @return A list of candidate evaluations with the specified locked status
     */
    List<CandidateEvaluationModel> findByLocked(boolean locked);
    
    /**
     * Find a candidate evaluation by resume ID and locked status.
     * 
     * @param resumeId The resume ID to search for
     * @param locked The locked status to filter by
     * @return An Optional containing the candidate evaluation if found, or empty if not found
     */
    Optional<CandidateEvaluationModel> findByResumeIdAndLocked(UUID resumeId, boolean locked);
    
    /**
     * Find a candidate evaluation by resume ID.
     * 
     * @param resumeId The resume ID to search for
     * @return An Optional containing the candidate evaluation if found, or empty if not found
     */
    Optional<CandidateEvaluationModel> findByResumeId(UUID resumeId);
}
