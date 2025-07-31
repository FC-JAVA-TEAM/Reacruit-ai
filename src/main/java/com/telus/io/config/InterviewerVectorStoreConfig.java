package com.telus.io.config;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.retry.support.RetryTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telus.io.model.InterviewerProfile;
import com.telus.io.model.InterviewerVectorStore;
import com.telus.io.repository.InterviewerProfileRepository;


/**
 * Configuration for the interviewer-specific vector store.
 * This class provides a JDBC-based implementation to handle empty vector store cases gracefully.
 */
@Configuration
public class InterviewerVectorStoreConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(InterviewerVectorStoreConfig.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private RetryTemplate aiRetryTemplate;
    
    @Autowired
    private InterviewerProfileRepository interviewerRepository;
    
    /**
     * Create a dedicated JDBC-based vector store service for interviewers.
     * 
     * @return A JDBC-based vector store service for interviewers
     */
    @Bean
    @Qualifier("interviewerVectorStoreService")
    public InterviewerVectorStoreService interviewerVectorStoreService() {
        return new InterviewerVectorStoreService(
                jdbcTemplate, 
                embeddingModel, 
                objectMapper, 
                aiRetryTemplate,
                interviewerRepository);
    }
    
    /**
     * Service class for interviewer vector store operations using JDBC.
     * This implementation handles empty vector store cases gracefully.
     */
    public static class InterviewerVectorStoreService {
        
        private static final Logger logger = LoggerFactory.getLogger(InterviewerVectorStoreService.class);
        
        private final JdbcTemplate jdbcTemplate;
        private final EmbeddingModel embeddingModel;
        private final ObjectMapper objectMapper;
        private final RetryTemplate retryTemplate;
        private final InterviewerProfileRepository interviewerRepository;
        
        public InterviewerVectorStoreService(
                JdbcTemplate jdbcTemplate, 
                EmbeddingModel embeddingModel, 
                ObjectMapper objectMapper,
                RetryTemplate retryTemplate,
                InterviewerProfileRepository interviewerRepository) {
            this.jdbcTemplate = jdbcTemplate;
            this.embeddingModel = embeddingModel;
            this.objectMapper = objectMapper;
            this.retryTemplate = retryTemplate;
            this.interviewerRepository = interviewerRepository;
        }
        
//        /**
//         * Find similar interviewer vector store entries with similarity scores.
//         * This method returns both the interviewer profiles and their similarity scores.
//         * 
//         * @param embedding The embedding vector to search for
//         * @param limit The maximum number of results to return
//         * @return A list of maps containing interviewer profiles and similarity scores
//         */
//        public List<Map<String, Object>> findSimilarWithScores1(String embedding, int limit) {
//            // First check if the vector store is empty
//            if (!existsAny()) {
//                logger.warn("Interviewer vector store is empty. Returning empty result.");
//                return Collections.emptyList();
//            }
//            
//            try {
//                // Perform similarity search with optimized query that includes similarity scores
//                // Note: PostgreSQL cosine distance is 1 - similarity, so we convert it to similarity
//                return jdbcTemplate.query(
//                    "SELECT ivs.id, ivs.interviewer_id, ivs.content, ivs.metadata, " +
//                    "1 - (ivs.embedding <=> ?::vector) AS similarity " +
//                    "FROM interviewer_vector_store ivs " +
//                    "ORDER BY ivs.embedding <=> ?::vector " +
//                    "LIMIT ?",
//                    (rs, rowNum) -> {
//                        Map<String, Object> result = new HashMap<>();
//                        
//                        // Get the interviewer ID and fetch the interviewer
//                        UUID interviewerId = UUID.fromString(rs.getString("interviewer_id"));
//                        InterviewerProfile interviewer = interviewerRepository.findById(interviewerId).orElse(null);
//                        
//                        // Get the similarity score (convert from distance)
//                        double similarity = rs.getDouble("similarity");
//                        
//                        // Add to result map
//                        result.put("interviewer", interviewer);
//                        result.put("similarity", similarity);
//                        
//                        return result;
//                    },
//                    embedding, embedding, limit);
//            } catch (DataAccessException e) {
//                logger.error("Error performing similarity search with scores: {}", e.getMessage(), e);
//                return Collections.emptyList();
//            }
//        }
        
        public List<Map<String, Object>> findSimilarWithScores(String embedding, int limit) {
            
            
            try {
                return jdbcTemplate.query(
                    "SELECT ivs.id, ivs.interviewer_id, ivs.content, ivs.metadata, " +
                    "1 - (ivs.embedding <=> ?::vector) AS similarity " +
                    "FROM interviewer_vector_store ivs " +
                    "ORDER BY ivs.embedding <=> ?::vector " +
                    "LIMIT ?",
                    (rs, rowNum) -> {
                        Map<String, Object> result = new HashMap<>();
                        
                        // Get metadata (contains ALL interviewer data)
                        String metadataJson = rs.getString("metadata");
                        Map<String, Object> metadata = parseMetadata(metadataJson);
                        
                        // Reconstruct InterviewerProfile from metadata (NO DB call!)
                        InterviewerProfile interviewer = reconstructInterviewerFromMetadata(metadata);
                        
                        // Get the similarity score
                        double similarity = rs.getDouble("similarity");
                        
                        // Add to result map
                        result.put("interviewer", interviewer);
                        result.put("similarity", similarity);
                        result.put("metadata", metadata);  // Keep metadata for debugging
                        
                        return result;
                    },
                    embedding, embedding, limit);
            } catch (DataAccessException e) {
                logger.error("Error performing similarity search with scores: {}", e.getMessage(), e);
                return Collections.emptyList();
            }
        }

        
        
        
        /**
         * Parse JSON metadata string into Map
         */
        private Map<String, Object> parseMetadata(String metadataJson) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(metadataJson, Map.class);
            } catch (Exception e) {
                logger.error("Error parsing metadata JSON: {}", e.getMessage(), e);
                return new HashMap<>();
            }
        }

        /**
         * Reconstruct InterviewerProfile from metadata (NO DB calls)
         */
        private InterviewerProfile reconstructInterviewerFromMetadata(Map<String, Object> metadata) {
            try {
                InterviewerProfile interviewer = new InterviewerProfile();
                
                // Set basic fields from your metadata
                interviewer.setId(UUID.fromString((String) metadata.get("interviewerId")));
                interviewer.setName((String) metadata.get("name"));
                interviewer.setEmail((String) metadata.get("email"));
                interviewer.setExperienceYears(((Number) metadata.get("experienceYears")).intValue());
                
                // Set tier if available
                if (metadata.containsKey("tier")) {
                   // interviewer.setTier(null);
                }
                
                // Set technical expertise
                Object techExpertise = metadata.get("technicalExpertise");
                if (techExpertise instanceof List) {
                    interviewer.setTechnicalExpertise((List<String>) techExpertise);
                }
                
                // Set specializations
                Object specializations = metadata.get("specializations");
                if (specializations instanceof List) {
                    interviewer.setSpecializations((List<String>) specializations);
                }
                
                return interviewer;
                
            } catch (Exception e) {
                logger.error("Error reconstructing interviewer from metadata: {}", e.getMessage(), e);
                return null;
            }
        }
        
        /**
         * Check if any interviewer vector store entries exist.
         * 
         * @return True if at least one entry exists, false otherwise
         */
        public boolean existsAny() {
            try {
                Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM interviewer_vector_store", 
                    Long.class);
                return count != null && count > 0;
            } catch (DataAccessException e) {
                logger.error("Error checking if interviewer vector store exists: {}", e.getMessage(), e);
                return false;
            }
        }
        
        /**
         * Count the total number of interviewer vector store entries.
         * 
         * @return The number of entries
         */
        public long countEntries() {
            try {
                Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM interviewer_vector_store", 
                    Long.class);
                return count != null ? count : 0;
            } catch (DataAccessException e) {
                logger.error("Error counting interviewer vector store entries: {}", e.getMessage(), e);
                return 0;
            }
        }
        
        /**
         * Find similar interviewer vector store entries.
         * This method handles empty vector store cases gracefully.
         * 
         * @param embedding The embedding vector to search for
         * @param limit The maximum number of results to return
         * @return A list of interviewer vector store entries ordered by similarity
         */
        public List<InterviewerVectorStore> findSimilar(String embedding, int limit) {
            // First check if the vector store is empty
            if (!existsAny()) {
                logger.warn("Interviewer vector store is empty. Returning empty result.");
                return Collections.emptyList();
            }
            
            try {
                // Perform similarity search with optimized query
                return jdbcTemplate.query(
                    "SELECT ivs.id, ivs.interviewer_id, ivs.content, ivs.metadata, ivs.embedding <=> ?::vector AS distance " +
                    "FROM interviewer_vector_store ivs " +
                    "ORDER BY ivs.embedding <=> ?::vector " +
                    "LIMIT ?",
                    new InterviewerVectorStoreRowMapper(),
                    embedding, embedding, limit);
            } catch (DataAccessException e) {
                logger.error("Error performing similarity search: {}", e.getMessage(), e);
                return Collections.emptyList();
            }
        }
        
        /**
         * Find similar interviewer vector store entries with a metadata filter.
         * This method handles empty vector store cases gracefully.
         * 
         * @param embedding The embedding vector to search for
         * @param metadataKey The metadata key to filter on
         * @param metadataValue The metadata value to filter on
         * @param limit The maximum number of results to return
         * @return A list of interviewer vector store entries ordered by similarity and filtered by metadata
         */
        public List<InterviewerVectorStore> findSimilarWithMetadataFilter(
                String embedding, String metadataKey, String metadataValue, int limit) {
            // First check if the vector store is empty
            if (!existsAny()) {
                logger.warn("Interviewer vector store is empty. Returning empty result.");
                return Collections.emptyList();
            }
            
            try {
                // Perform similarity search with metadata filter
                return jdbcTemplate.query(
                    "SELECT ivs.id, ivs.interviewer_id, ivs.content, ivs.metadata, ivs.embedding <=> ?::vector AS distance " +
                    "FROM interviewer_vector_store ivs " +
                    "WHERE ivs.metadata->? = CAST(? AS jsonb) " +
                    "ORDER BY ivs.embedding <=> ?::vector " +
                    "LIMIT ?",
                    new InterviewerVectorStoreRowMapper(),
                    embedding, metadataKey, metadataValue, embedding, limit);
            } catch (DataAccessException e) {
                logger.error("Error performing similarity search with metadata filter: {}", e.getMessage(), e);
                return Collections.emptyList();
            }
        }
        
        /**
         * Generate an embedding for the given text.
         * 
         * @param text The text to generate an embedding for
         * @return The embedding as a float array
         */
        public float[] generateEmbedding(String text) {
            return retryTemplate.execute(context -> {
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
        public String convertToVectorString(float[] embedding) {
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
        
        /**
         * Row mapper for interviewer vector store entries.
         */
        private class InterviewerVectorStoreRowMapper implements RowMapper<InterviewerVectorStore> {
            
            @Override
            public InterviewerVectorStore mapRow(ResultSet rs, int rowNum) throws SQLException {
                UUID id = UUID.fromString(rs.getString("id"));
                UUID interviewerId = UUID.fromString(rs.getString("interviewer_id"));
                String content = rs.getString("content");
                
                // Get metadata and handle different types
                Object metadataObj = rs.getObject("metadata");
                Map<String, Object> metadata = parseMetadata(metadataObj);
                
                // Create a new InterviewerVectorStore
                InterviewerVectorStore vectorStore = new InterviewerVectorStore();
                vectorStore.setId(id);
                
                // Fetch the interviewer
                interviewerRepository.findById(interviewerId).ifPresent(vectorStore::setInterviewer);
                
                vectorStore.setContent(content);
                vectorStore.setMetadata(metadata);
                
                return vectorStore;
            }
            
            /**
             * Parse metadata from various object types.
             * 
             * @param metadataObj The metadata object
             * @return The parsed metadata as a map
             */
            private Map<String, Object> parseMetadata(Object metadataObj) {
                String metadataStr;
                
                if (metadataObj instanceof PGobject) {
                    // If it's a PGobject (PostgreSQL's JSON type), get its string value
                    metadataStr = ((PGobject) metadataObj).getValue();
                } else if (metadataObj instanceof String) {
                    // If it's already a string, use it directly
                    metadataStr = (String) metadataObj;
                } else if (metadataObj != null) {
                    // For any other non-null type, use toString()
                    metadataStr = metadataObj.toString();
                    logger.warn("Unexpected metadata type: {}", metadataObj.getClass().getName());
                } else {
                    // Handle null case
                    metadataStr = "{}";
                    logger.warn("Null metadata found in search results");
                }
                
                // Parse metadata JSON back to a Map
                try {
                    // Try to parse the JSON string back to a Map
                    return objectMapper.readValue(metadataStr, 
                            objectMapper.getTypeFactory().constructMapType(
                                    Map.class, String.class, Object.class));
                } catch (Exception e) {
                    logger.error("Error parsing metadata JSON: {}", e.getMessage(), e);
                    // Fallback to empty map if parsing fails
                    return new HashMap<>();
                }
            }
        }
    }
}
