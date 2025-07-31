package com.telus.io.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Configuration for prompt templates.
 */
@Configuration
public class PromptTemplateConfig {

    private static final Logger logger = LoggerFactory.getLogger(PromptTemplateConfig.class);

@Value("classpath:prompts/resume-match.prompt")
private Resource resumeMatchPromptResource;

@Value("classpath:prompts/interviewer-match-explanation.prompt")
private Resource interviewerMatchExplanationPromptResource;

@Value("classpath:prompts/job-description-match.prompt")
private Resource jobDescriptionMatchPromptResource;

@Value("classpath:prompts/job-description-generate.prompt")
private Resource jobDescriptionGeneratePromptResource;

@Value("classpath:prompts/interview-questions-generate.prompt")
private Resource interviewQuestionsGeneratePromptResource;
    
    /**
     * Load the resume match prompt template.
     */
    @Bean(name = "resumeMatchPrompt")
    public String resumeMatchPrompt() throws IOException {
        logger.info("Loading resume match prompt template");
        return loadTemplate(resumeMatchPromptResource);
    }
    
/**
 * Load the interviewer match explanation prompt template.
 */
@Bean(name = "interviewerMatchExplanationPrompt")
public String interviewerMatchExplanationPrompt() throws IOException {
    logger.info("Loading interviewer match explanation prompt template");
    return loadTemplate(interviewerMatchExplanationPromptResource);
}

/**
 * Load the job description match prompt template.
 */
@Bean(name = "jobDescriptionMatchPrompt")
public String jobDescriptionMatchPrompt() throws IOException {
    logger.info("Loading job description match prompt template");
    return loadTemplate(jobDescriptionMatchPromptResource);
}

/**
 * Load the job description generation prompt template.
 */
@Bean(name = "jobDescriptionGeneratePrompt")
public String jobDescriptionGeneratePrompt() throws IOException {
    logger.info("Loading job description generation prompt template");
    return loadTemplate(jobDescriptionGeneratePromptResource);
}

/**
 * Load the interview questions generation prompt template.
 */
@Bean(name = "interviewQuestionsGeneratePrompt")
public String interviewQuestionsGeneratePrompt() throws IOException {
    logger.info("Loading interview questions generation prompt template");
    return loadTemplate(interviewQuestionsGeneratePromptResource);
}
    
    /**
     * Load a template from a resource.
     */
    private String loadTemplate(Resource resource) throws IOException {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }
    }
    
    // Note: The ResumeAnalysisConverter is now auto-configured via @Component annotation
}
