package com.telus.io.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telus.io.dto.request.LockResumeRequest;
import com.telus.io.exception.ResourceNotFoundException;
import com.telus.io.model.CandidateEvaluationModel;
import com.telus.io.model.Resume;
import com.telus.io.repository.ResumeRepository;
import com.telus.io.service.CandidateEvaluationService;



/**
 * REST controller for managing resume lock/unlock operations.
 */
@RestController
@RequestMapping("/api/resume-locks")
public class ResumeLockController {
    
    private static final Logger logger = LoggerFactory.getLogger(ResumeLockController.class);
    
    private final CandidateEvaluationService evaluationService;
    private final ResumeRepository resumeRepository;
    
    @Autowired
    public ResumeLockController(CandidateEvaluationService evaluationService, 
                               ResumeRepository resumeRepository) {
        this.evaluationService = evaluationService;
        this.resumeRepository = resumeRepository;
    }
    
    
    
    @GetMapping("/hello")
  //  @CrossOrigin(origins = "*")
    public ResponseEntity<String> helloWorld() {
        logger.info("Hello World endpoint accessed");
        return ResponseEntity.ok("Hello World from Resume Lock Controller!");
    }
    
    /**
     * Lock, unlock, or update a resume for a specific manager.
     * 
     * @param request The lock resume request containing resume ID, manager ID, and optional evaluation data
     * @return The updated candidate evaluation with locked status
     * @throws CandidateAlreadyLockedException if the resume is already locked by another manager
     */
    @PostMapping("/lock")
    public ResponseEntity<CandidateEvaluationModel> lockResume(@RequestBody LockResumeRequest request) {
        logger.debug("Received lock request for resume ID: {}, manager ID: {}", request.getResumeId(), request.getManagerId());
        
        // Use the service to process the lock request
        CandidateEvaluationModel result = evaluationService.processLockRequest(request);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Unlock a resume that was previously locked by a manager.
     * 
     * @param request The lock resume request containing resume ID and manager ID
     * @return The updated candidate evaluation with unlocked status
     */
    @PostMapping("/unlock")
    public ResponseEntity<CandidateEvaluationModel> unlockResume(@RequestBody LockResumeRequest request) {
        logger.debug("Unlocking resume ID: {} by manager: {}", request.getResumeId(), request.getManagerId());
        
        // Set locked to false and process the request
        request.setLocked(false);
        CandidateEvaluationModel result = evaluationService.processLockRequest(request);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get the lock status of a resume.
     * 
     * @param resumeId The ID of the resume to check
     * @return The candidate evaluation DTO with lock information
     */
    @GetMapping("/{resumeId}")
    public ResponseEntity<CandidateEvaluationModel> getResumeLockStatus(@PathVariable UUID resumeId) {
        logger.debug("Getting lock status for resume ID: {}", resumeId);
        
        // Check if resume exists
        Resume resume = resumeRepository.findById(resumeId)
            .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId));
        
        // Check if there's an evaluation record for this resume
        Optional<CandidateEvaluationModel> existingEvaluation = evaluationService.findByResumeId(resumeId);
        
        if (existingEvaluation.isPresent()) {
            return ResponseEntity.ok(existingEvaluation.get());
        } else {
            // Create a basic DTO with resume info
            CandidateEvaluationModel dto = new CandidateEvaluationModel();
            dto.setResumeId(resumeId);
            dto.setName(resume.getName());
            dto.setEmail(resume.getEmail());
            dto.setPhoneNumber(resume.getPhoneNumber());
            dto.setLocked(false);
            
            return ResponseEntity.ok(dto);
        }
    }
    
    /**
     * Get all locked resumes.
     * 
     * @return A list of all locked candidate evaluations
     */
    @GetMapping("/locked")
    public ResponseEntity<List<CandidateEvaluationModel>> getAllLockedResumes() {
        logger.debug("Getting all locked resumes");
        List<CandidateEvaluationModel> lockedResumes = evaluationService.findByLocked(true);
        return ResponseEntity.ok(lockedResumes);
    }
}
