package com.telus.io.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telus.io.config.InterviewerVectorStoreConfig.InterviewerVectorStoreService;
import com.telus.io.exception.ResourceNotFoundException;
import com.telus.io.model.InterviewerProfile;
import com.telus.io.model.InterviewerVectorStore;
import com.telus.io.repository.InterviewerProfileRepository;
import com.telus.io.repository.InterviewerVectorStoreRepository;
import com.telus.io.service.InterviewerProfileService;


/**
 * Implementation of the InterviewerProfileService interface.
 */
@Service
public class InterviewerProfileServiceImpl implements InterviewerProfileService {
    
    private static final Logger logger = LoggerFactory.getLogger(InterviewerProfileServiceImpl.class);
    
    private final InterviewerProfileRepository interviewerRepository;
    private final InterviewerVectorStoreRepository vectorStoreRepository;
    private final InterviewerVectorStoreService vectorStoreService;
    private final EmbeddingModel embeddingModel;
    private final RetryTemplate aiRetryTemplate;
   // private final ObjectMapper objectMapper;
    
    @Autowired
    public InterviewerProfileServiceImpl(
            InterviewerProfileRepository interviewerRepository,
            InterviewerVectorStoreRepository vectorStoreRepository,
            @Qualifier("interviewerVectorStoreService") InterviewerVectorStoreService vectorStoreService,
            EmbeddingModel embeddingModel,
            RetryTemplate aiRetryTemplate,
            ObjectMapper objectMapper) {
        this.interviewerRepository = interviewerRepository;
        this.vectorStoreRepository = vectorStoreRepository;
        this.vectorStoreService = vectorStoreService;
        this.embeddingModel = embeddingModel;
        this.aiRetryTemplate = aiRetryTemplate;
       // this.objectMapper = objectMapper;
    }
    
    @Override
    @Transactional
    public InterviewerProfile createInterviewer(InterviewerProfile interviewer) {
        InterviewerProfile savedInterviewer = interviewerRepository.save(interviewer);
        updateVectorStore(savedInterviewer);
        return savedInterviewer;
    }
    
    @Override
    @Transactional
    public InterviewerProfile updateInterviewer(UUID id, InterviewerProfile interviewer) {
        InterviewerProfile existingInterviewer = interviewerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interviewer", "id", id));
        
        // Update fields
        existingInterviewer.setName(interviewer.getName());
        existingInterviewer.setEmail(interviewer.getEmail());
        existingInterviewer.setPhoneNumber(interviewer.getPhoneNumber());
        existingInterviewer.setExperienceYears(interviewer.getExperienceYears());
        existingInterviewer.setInterviewerTier(interviewer.getInterviewerTier());
        existingInterviewer.setMaxInterviewsPerDay(interviewer.getMaxInterviewsPerDay());
        existingInterviewer.setTechnicalExpertise(interviewer.getTechnicalExpertise());
        existingInterviewer.setSpecializations(interviewer.getSpecializations());
        
        // Only update availability if provided
        if (interviewer.getAvailability() != null) {
            existingInterviewer.setAvailability(interviewer.getAvailability());
        }
        
        InterviewerProfile updatedInterviewer = interviewerRepository.save(existingInterviewer);
        
        // Update vector store
        updateVectorStore(updatedInterviewer);
        
        return updatedInterviewer;
    }
    
    @Override
    public Optional<InterviewerProfile> getInterviewerById(UUID id) {
        return interviewerRepository.findById(id);
    }
    
    @Override
    public Optional<InterviewerProfile> getInterviewerByEmail(String email) {
        return interviewerRepository.findByEmail(email);
    }
    
    @Override
    public List<InterviewerProfile> getAllInterviewers() {
        return interviewerRepository.findAll();
    }
    
    @Override
    @Transactional
    public void deleteInterviewer(UUID id) {
        // Delete from vector store first
        vectorStoreRepository.deleteByInterviewerId(id);
        
        // Then delete the interviewer
        interviewerRepository.deleteById(id);
    }
    
    @Override
    public List<InterviewerProfile> findInterviewersByExpertise(String expertise) {
        try {
            // Format as JSON array for PostgreSQL JSONB containment operator
            String jsonExpertise = String.format("[\"%s\"]", expertise);
            return interviewerRepository.findByTechnicalExpertise(jsonExpertise);
        } catch (Exception e) {
            logger.error("Error finding interviewers by expertise: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<InterviewerProfile> findInterviewersBySpecialization(String specialization) {
        try {
            // Format as JSON array for PostgreSQL JSONB containment operator
            String jsonSpecialization = String.format("[\"%s\"]", specialization);
            return interviewerRepository.findBySpecialization(jsonSpecialization);
        } catch (Exception e) {
            logger.error("Error finding interviewers by specialization: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<InterviewerProfile> findInterviewersByTier(int tier) {
        return interviewerRepository.findByInterviewerTier(tier);
    }
    
    @Override
    public List<InterviewerProfile> findInterviewersByMinimumExperience(int years) {
        return interviewerRepository.findByMinimumExperienceYears(years);
    }
    
    @Override
    @Transactional
    public InterviewerProfile updateInterviewerAvailability(UUID id, Map<String, Integer> availability) {
        InterviewerProfile interviewer = interviewerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interviewer", "id", id));
        
        interviewer.setAvailability(availability);
        return interviewerRepository.save(interviewer);
    }
    
    @Override
    public List<InterviewerProfile> findInterviewersBySimilarity(String query, int limit) {
        try {
            // Generate embedding for the query
            float[] queryEmbedding = generateEmbedding(query);
            String vectorString = convertToVectorString(queryEmbedding);
            
            // Use the JDBC-based service for similarity search
            List<InterviewerVectorStore> results = vectorStoreService.findSimilar(vectorString, limit);
            
            // Extract interviewer profiles
            return results.stream()
                    .map(InterviewerVectorStore::getInterviewer)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error performing similarity search: {}", e.getMessage(), e);
            // Return empty list in case of error
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<InterviewerProfile> findInterviewersBySimilarityWithFilters(
            String query, Map<String, String> filters, int limit) {
        if (filters == null || filters.isEmpty()) {
            return findInterviewersBySimilarity(query, limit);
        }
        
        try {
            // Generate embedding for the query
            float[] queryEmbedding = generateEmbedding(query);
            String vectorString = convertToVectorString(queryEmbedding);
            
            // For simplicity, we'll just use the first filter
            Map.Entry<String, String> firstFilter = filters.entrySet().iterator().next();
            String metadataKey = firstFilter.getKey();
            String metadataValue = firstFilter.getValue();
            
            // Use the JDBC-based service for similarity search with filter
            List<InterviewerVectorStore> results = vectorStoreService.findSimilarWithMetadataFilter(
                    vectorString, metadataKey, metadataValue, limit);
            
            // Extract interviewer profiles
            return results.stream()
                    .map(InterviewerVectorStore::getInterviewer)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error performing similarity search with filters: {}", e.getMessage(), e);
            // Return empty list in case of error
            return new ArrayList<>();
        }
    }
    
    /**
     * Update the vector store for an interviewer.
     * 
     * @param interviewer The interviewer profile to update in the vector store
     */
    private void updateVectorStore(InterviewerProfile interviewer) {
        try {
            // Delete existing vector store entries for this interviewer
            vectorStoreRepository.deleteByInterviewerId(interviewer.getId());
            
            // Create document for vector store
            String content = createContentForVectorStore(interviewer);
            Map<String, Object> metadata = createMetadataForVectorStore(interviewer);
            
            Document document = new Document(content, metadata);
            
            // Generate embedding
            float[] embedding = generateEmbedding(content);
            
            // Create vector store entry
            InterviewerVectorStore vectorStore = new InterviewerVectorStore(
                    interviewer, content, metadata, embedding);
            
            // Save to vector store
            vectorStoreRepository.save(vectorStore);
            
        } catch (Exception e) {
            logger.error("Error updating vector store for interviewer: {}", e.getMessage(), e);
            // Continue without failing the transaction
        }
    }
    
    /**
     * Create content for the vector store from an interviewer profile.
     * 
     * @param interviewer The interviewer profile
     * @return The content for the vector store
     */
    private String createContentForVectorStore(InterviewerProfile interviewer) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Name: ").append(interviewer.getName()).append("\n");
        sb.append("Email: ").append(interviewer.getEmail()).append("\n");
        sb.append("Experience: ").append(interviewer.getExperienceYears()).append(" years\n");
        sb.append("Tier: ").append(interviewer.getInterviewerTier()).append("\n");
        
        sb.append("Technical Expertise: ");
        if (interviewer.getTechnicalExpertise() != null) {
            sb.append(String.join(", ", interviewer.getTechnicalExpertise()));
        }
        sb.append("\n");
        
        sb.append("Specializations: ");
        if (interviewer.getSpecializations() != null) {
            sb.append(String.join(", ", interviewer.getSpecializations()));
        }
        sb.append("\n");
        
        return sb.toString();
    }
    
    /**
     * Create metadata for the vector store from an interviewer profile.
     * 
     * @param interviewer The interviewer profile
     * @return The metadata for the vector store
     */
    private Map<String, Object> createMetadataForVectorStore(InterviewerProfile interviewer) {
        Map<String, Object> metadata = new HashMap<>();
        
        metadata.put("interviewerId", interviewer.getId().toString());
        metadata.put("name", interviewer.getName());
        metadata.put("email", interviewer.getEmail());
        metadata.put("experienceYears", interviewer.getExperienceYears());
        metadata.put("tier", interviewer.getInterviewerTier());
        
        if (interviewer.getTechnicalExpertise() != null) {
            metadata.put("technicalExpertise", interviewer.getTechnicalExpertise());
        }
        
        if (interviewer.getSpecializations() != null) {
            metadata.put("specializations", interviewer.getSpecializations());
        }
        
        return metadata;
    }
    
    /**
     * Generate an embedding for the given text.
     * 
     * @param text The text to generate an embedding for
     * @return The embedding
     */
    private float[] generateEmbedding(String text) {
        return aiRetryTemplate.execute(context -> {
            try {
                return embeddingModel.embed(text);
            } catch (Exception e) {
                logger.error("Error generating embedding: {}", e.getMessage());
                throw e;
            }
        }, context -> {
            // Fallback when all retries fail
            logger.error("All retries failed for embedding generation. Using fallback empty embedding.");
            return new float[1536]; // Default embedding dimension
        });
    }
    
    /**
     * Convert a float array to a PostgreSQL vector string.
     * 
     * @param embedding The embedding to convert
     * @return The vector string
     */
    private String convertToVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
