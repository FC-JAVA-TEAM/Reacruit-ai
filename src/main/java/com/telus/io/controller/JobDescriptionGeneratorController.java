package com.telus.io.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telus.io.dto.JobDescriptionDTO;
import com.telus.io.dto.response.ApiResponse;


/**
 * Controller for generating job descriptions using AI.
 */
@RestController
@RequestMapping("/api/job-descriptions")
public class JobDescriptionGeneratorController {
    
    private static final Logger logger = LoggerFactory.getLogger(JobDescriptionGeneratorController.class);
    
    private final ChatClient chatClient;
    private final String jobDescriptionGeneratePrompt;
    private final String interviewQuestionsGeneratePrompt;
    
    public JobDescriptionGeneratorController(
            ChatClient chatClient,
            @Qualifier("jobDescriptionGeneratePrompt") String jobDescriptionGeneratePrompt,
            @Qualifier("interviewQuestionsGeneratePrompt") String interviewQuestionsGeneratePrompt) {
        this.chatClient = chatClient;
        this.jobDescriptionGeneratePrompt = jobDescriptionGeneratePrompt;
        this.interviewQuestionsGeneratePrompt = interviewQuestionsGeneratePrompt;
    }
    
    /**
     * Generate a job description from a simple prompt.
     * 
     * @param prompt A simple description of the job (e.g., "Senior Java Developer with Spring Boot experience")
     * @return A response containing the structured job description
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse> generateJobDescription(@RequestBody String prompt) {
        logger.info("Generating job description from prompt: {}", prompt);
        
        if (prompt == null || prompt.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                new ApiResponse(false, "Prompt cannot be empty", null));
        }
        
        try {
            // Use the prompt template as the system message
            String systemPrompt = jobDescriptionGeneratePrompt;
            
            JobDescriptionDTO generatedJobDescriptionJson = chatClient.prompt()
                .system(systemPrompt)
                .user(prompt)
                .call()
                .entity(JobDescriptionDTO.class);
            
            logger.info("Successfully generated job description JSON");
            
            return ResponseEntity.ok(
                new ApiResponse(true, "Job description generated successfully", generatedJobDescriptionJson));
        } catch (Exception e) {
            logger.error("Error generating job description", e);
            return ResponseEntity.internalServerError().body(
                new ApiResponse(false, "Error generating job description: " + e.getMessage(), null));
        }
    }
    
    /**
     * Generate interview questions based on a job description.
     * 
     * @param jobDescription The job description to generate questions for
     * @return A response containing the generated interview questions
     */
    @PostMapping("/generate-questions")
    public ResponseEntity<ApiResponse> generateInterviewQuestions(@RequestBody String jobDescription) {
        logger.info("Generating interview questions for job description");
        
        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                new ApiResponse(false, "Job description cannot be empty", null));
        }
        
        try {
            // Use the interview questions prompt template as the system message
            String systemPrompt = interviewQuestionsGeneratePrompt;
            
            // Generate the interview questions using the AI
            String generatedQuestions = chatClient.prompt()
                .system(systemPrompt)
                .user(jobDescription)  // Pass the job description as a user message
                .call()
                .content();
            
            logger.info("Successfully generated interview questions");
            
            // Return the generated interview questions
            return ResponseEntity.ok(
                new ApiResponse(true, "Interview questions generated successfully", generatedQuestions));
        } catch (Exception e) {
            logger.error("Error generating interview questions", e);
            return ResponseEntity.internalServerError().body(
                new ApiResponse(false, "Error generating interview questions: " + e.getMessage(), null));
        }
    }
    
    /**
     * Generate interview questions based on a structured job description.
     * 
     * @param jobDescription The structured job description to generate questions for
     * @return A response containing the generated interview questions
     */
    @PostMapping("/generate-questions-from-dto")
    public ResponseEntity<ApiResponse> generateInterviewQuestionsFromDTO(@RequestBody JobDescriptionDTO jobDescription) {
        logger.info("Generating interview questions for structured job description");
        
        if (jobDescription == null) {
            return ResponseEntity.badRequest().body(
                new ApiResponse(false, "Job description cannot be empty", null));
        }
        
        try {
            // Convert the DTO to a string representation for the AI
            StringBuilder jobDescriptionText = new StringBuilder();
            jobDescriptionText.append("Title: ").append(jobDescription.getTitle()).append("\n\n");
            
            if (jobDescription.getCompany() != null) {
                jobDescriptionText.append("Company: ").append(jobDescription.getCompany()).append("\n\n");
            }
            
            if (jobDescription.getLocation() != null) {
                jobDescriptionText.append("Location: ").append(jobDescription.getLocation()).append("\n\n");
            }
            
            if (jobDescription.getSummary() != null) {
                jobDescriptionText.append("Summary: ").append(jobDescription.getSummary()).append("\n\n");
            }
            
            if (jobDescription.getResponsibilities() != null && !jobDescription.getResponsibilities().isEmpty()) {
                jobDescriptionText.append("Responsibilities:\n");
                for (String responsibility : jobDescription.getResponsibilities()) {
                    jobDescriptionText.append("- ").append(responsibility).append("\n");
                }
                jobDescriptionText.append("\n");
            }
            
            if (jobDescription.getRequiredQualifications() != null && !jobDescription.getRequiredQualifications().isEmpty()) {
                jobDescriptionText.append("Required Qualifications:\n");
                for (String qualification : jobDescription.getRequiredQualifications()) {
                    jobDescriptionText.append("- ").append(qualification).append("\n");
                }
                jobDescriptionText.append("\n");
            }
            
            if (jobDescription.getPreferredQualifications() != null && !jobDescription.getPreferredQualifications().isEmpty()) {
                jobDescriptionText.append("Preferred Qualifications:\n");
                for (String qualification : jobDescription.getPreferredQualifications()) {
                    jobDescriptionText.append("- ").append(qualification).append("\n");
                }
                jobDescriptionText.append("\n");
            }
            
            if (jobDescription.getTechnicalSkills() != null && !jobDescription.getTechnicalSkills().isEmpty()) {
                jobDescriptionText.append("Technical Skills: ").append(String.join(", ", jobDescription.getTechnicalSkills())).append("\n\n");
            }
            
            // Use the interview questions prompt template as the system message
            String systemPrompt = interviewQuestionsGeneratePrompt;
            
            // Generate the interview questions using the AI
            String generatedQuestions = chatClient.prompt()
                .system(systemPrompt)
                .user(jobDescriptionText.toString())
                .call()
                .content();
            
            logger.info("Successfully generated interview questions from structured job description");
            
            // Return the generated interview questions
            return ResponseEntity.ok(
                new ApiResponse(true, "Interview questions generated successfully", generatedQuestions));
        } catch (Exception e) {
            logger.error("Error generating interview questions", e);
            return ResponseEntity.internalServerError().body(
                new ApiResponse(false, "Error generating interview questions: " + e.getMessage(), null));
        }
    }
}
