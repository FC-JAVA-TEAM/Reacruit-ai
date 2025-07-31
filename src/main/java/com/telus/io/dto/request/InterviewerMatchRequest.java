package com.telus.io.dto.request;

import java.util.UUID;

/**
 * Request DTO for finding matching interviewers based on either a resume ID or an evaluation ID.
 */
public class InterviewerMatchRequest {
    private UUID resumeId;
    private UUID evaluationId;
    private Integer limit;
    
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
    
    public Integer getLimit() {
        return limit;
    }
    
    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
