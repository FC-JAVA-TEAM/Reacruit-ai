package com.telus.io.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for interviewer matching.
 */
@Configuration
public class InterviewerMatchingConfig {
    
    /**
     * The similarity threshold for interviewer matching.
     * Values range from 0.0 to 1.0, where higher values mean stricter matching.
     * Default value is 0.5.
     */
    @Value("${interviewer.matching.similarity-threshold:0.7}")
    private double similarityThreshold;
    
    /**
     * Whether dual vector matching is enabled.
     * When enabled, both resume and interviewer vector stores are used for matching.
     * Default value is true.
     */
    @Value("${interviewer.matching.dual-vector.enabled:true}")
    private boolean dualVectorEnabled;
    
    /**
     * The weight to apply to the resume vector store similarity score.
     * Values range from 0.0 to 1.0, where higher values give more importance to resume matching.
     * Default value is 0.5 (equal weight).
     */
    @Value("${interviewer.matching.dual-vector.resume-weight:0.5}")
    private double resumeWeight;
    
    /**
     * The weight to apply to the interviewer vector store similarity score.
     * Values range from 0.0 to 1.0, where higher values give more importance to interviewer matching.
     * Default value is 0.5 (equal weight).
     */
    @Value("${interviewer.matching.dual-vector.interviewer-weight:0.5}")
    private double interviewerWeight;
    
    /**
     * The similarity threshold for dual vector matching.
     * Values range from 0.0 to 1.0, where higher values mean stricter matching.
     * Default value is 0.6.
     */
    @Value("${interviewer.matching.dual-vector.threshold:0.6}")
    private double dualVectorThreshold;
    
    /**
     * Get the similarity threshold for interviewer matching.
     * 
     * @return The similarity threshold (0.0 to 1.0)
     */
    public double getSimilarityThreshold() {
        return similarityThreshold;
    }
    
    /**
     * Check if dual vector matching is enabled.
     * 
     * @return True if dual vector matching is enabled, false otherwise
     */
    public boolean isDualVectorEnabled() {
        return dualVectorEnabled;
    }
    
    /**
     * Get the weight to apply to the resume vector store similarity score.
     * 
     * @return The resume weight (0.0 to 1.0)
     */
    public double getResumeWeight() {
        return resumeWeight;
    }
    
    /**
     * Get the weight to apply to the interviewer vector store similarity score.
     * 
     * @return The interviewer weight (0.0 to 1.0)
     */
    public double getInterviewerWeight() {
        return interviewerWeight;
    }
    
    /**
     * Get the similarity threshold for dual vector matching.
     * 
     * @return The dual vector threshold (0.0 to 1.0)
     */
    public double getDualVectorThreshold() {
        return dualVectorThreshold;
    }
}
