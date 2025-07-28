package com.telus.io.exception;

import java.util.UUID;

/**
 * Exception thrown when a manager tries to lock or modify a resume that is already locked by another manager.
 */
public class CandidateAlreadyLockedException extends RuntimeException {
    
    private final UUID resumeId;
    private final String lockingManagerId;
    
    /**
     * Constructs a new CandidateAlreadyLockedException with the specified resume ID and locking manager ID.
     * 
     * @param resumeId The ID of the resume that is already locked
     * @param lockingManagerId The ID of the manager who has locked the resume
     */
    public CandidateAlreadyLockedException(UUID resumeId, String lockingManagerId) {
        super(String.format("Resume with ID %s is already locked by manager: %s", resumeId, lockingManagerId));
        this.resumeId = resumeId;
        this.lockingManagerId = lockingManagerId;
    }
    
    /**
     * Gets the ID of the resume that is already locked.
     * 
     * @return The resume ID
     */
    public UUID getResumeId() {
        return resumeId;
    }
    
    /**
     * Gets the ID of the manager who has locked the resume.
     * 
     * @return The locking manager ID
     */
    public String getLockingManagerId() {
        return lockingManagerId;
    }
}
