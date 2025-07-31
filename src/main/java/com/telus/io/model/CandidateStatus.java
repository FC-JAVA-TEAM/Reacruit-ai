package com.telus.io.model;

/**
 * Enum representing the possible statuses of a candidate in the hiring process.
 */
public enum CandidateStatus {
    /**
     * Initial status when a candidate is first evaluated.
     */
    OPEN,
    
    /**
     * Candidate is being reviewed by a hiring manager.
     */
    LOCKED,
    
    /**
     * Candidate has been shortlisted for further consideration.
     */
    SHORTLISTED,
    
    /**
     * Candidate has been contacted for an interview.
     */
    CONTACTED,
    
    /**
     * Candidate has been interviewed.
     */
    INTERVIEWED,
    
    /**
     * Candidate has been offered a position.
     */
    OFFERED,
    
    /**
     * Candidate has accepted an offer.
     */
    ACCEPTED,
    
    /**
     * Candidate has been rejected.
     */
    REJECTED,
    
    /**
     * Candidate has withdrawn from the process.
     */
    WITHDRAWN,
    
    /**
     * Candidate has been hired.
     */
    HIRED,
    
    /**
     * Custom status for special cases.
     */
    CUSTOM,
    
    INITIATE
}
