package com.telus.io.config;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.support.RetryTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configuration for the resume-specific vector store.
 */
@Configuration
public class ResumeVectorStoreConfig {
    
    /**
     * Create a dedicated vector store for resumes.
     * 
     * @param jdbcTemplate The JDBC template for database operations
     * @param embeddingModel The embedding model for generating embeddings
     * @return A vector store for resumes
     */
    @Bean
    @Qualifier("resumeVectorStore")
    public VectorStore resumeVectorStore(
            JdbcTemplate jdbcTemplate, 
            EmbeddingModel embeddingModel, 
            ObjectMapper objectMapper,
            RetryTemplate aiRetryTemplate) {
        return new ResumeVectorStore(jdbcTemplate, embeddingModel, objectMapper, aiRetryTemplate);
    }
    
    /**
     * Custom implementation of VectorStore that uses the resume_vector_store table.
     */
    public static class ResumeVectorStore implements VectorStore {
        
        private static final Logger logger = LoggerFactory.getLogger(ResumeVectorStore.class);
        
        private final JdbcTemplate jdbcTemplate;
        private final EmbeddingModel embeddingModel;
        private final ObjectMapper objectMapper;
        private final RetryTemplate retryTemplate;
        
        public ResumeVectorStore(
                JdbcTemplate jdbcTemplate, 
                EmbeddingModel embeddingModel, 
                ObjectMapper objectMapper,
                RetryTemplate retryTemplate) {
            this.jdbcTemplate = jdbcTemplate;
            this.embeddingModel = embeddingModel;
            this.objectMapper = objectMapper;
            this.retryTemplate = retryTemplate;
        }
        
        @Override
        public void add(List<Document> documents) {
            for (Document document : documents) {
                try {
                    // Generate embedding with retry for network issues
                    float[] embedding = retryTemplate.execute(context -> {
                        try {
                            return embeddingModel.embed(document);
                        } catch (Exception e) {
                            logger.error("Error generating embedding: {}", e.getMessage());
                            throw e;
                        }
                    }, context -> {
                        // Fallback when all retries fail
                        logger.error("All retries failed for embedding generation. Using fallback empty embedding.");
                        return new float[1536]; // Default embedding dimension
                    });
                    
                    // Convert embedding to PostgreSQL vector format
                    String vectorString = convertToVectorString(embedding);
                    
                    // Get resumeId as string and convert to UUID
                    String resumeIdStr = document.getMetadata().get("resumeId").toString();
                    UUID resumeId = UUID.fromString(resumeIdStr);
                    
                    // Convert metadata to proper JSON string using ObjectMapper
                    String metadataJson;
                    try {
                        metadataJson = objectMapper.writeValueAsString(document.getMetadata());
                    } catch (JsonProcessingException e) {
                        logger.error("Error converting metadata to JSON: {}", e.getMessage());
                        // Fallback to empty JSON object if conversion fails
                        metadataJson = "{}";
                    }
                    
                    // Insert into resume_vector_store table
                    jdbcTemplate.update(
                        "INSERT INTO resume_vector_store (id, resume_id, content, metadata, embedding) VALUES (?, ?, ?, ?::json, ?::vector)",
                        UUID.randomUUID(),
                        resumeId,  // Now it's a UUID
                        document.getText(),
                        metadataJson,  // Properly formatted JSON
                        vectorString
                    );
                } catch (Exception e) {
                    logger.error("Error adding document to vector store: {}", e.getMessage(), e);
                    // Continue with the next document
                }
            }
        }
        
        
        
        @Override
        public List<Document> similaritySearch(SearchRequest request) {
            // Generate embedding for the query with retry for network issues
            float[] queryEmbedding = retryTemplate.execute(context -> {
                try {
                    return embeddingModel.embed(request.getQuery());
                } catch (Exception e) {
                    logger.error("Error generating query embedding: {}", e.getMessage());
                    throw e;
                }
            }, context -> {
                // Fallback when all retries fail
                logger.error("All retries failed for query embedding generation. Using fallback empty embedding.");
                return new float[1536]; // Default embedding dimension
            });
            
            // Convert embedding to PostgreSQL vector format
            String vectorString = convertToVectorString(queryEmbedding);
            
            // Perform similarity search with optimized query
            // Added index hint and optimized the query for better performance
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT id, resume_id, content, metadata, embedding <=> ?::vector AS distance " +
                "FROM resume_vector_store " +
                "ORDER BY embedding <=> ?::vector " +
                "LIMIT ?",
                vectorString,
                vectorString,
                request.getTopK()
            );
            
            // Convert results to Document objects
            return results.stream()
                .map(row -> {
                    String content = (String) row.get("content");
                    
                    // Get metadata and handle different types
                    Object metadataObj = row.get("metadata");
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
                    Map<String, Object> metadata;
                    try {
                        // Try to parse the JSON string back to a Map
                        metadata = objectMapper.readValue(metadataStr, 
                                objectMapper.getTypeFactory().constructMapType(
                                        Map.class, String.class, Object.class));
                    } catch (Exception e) {
                        logger.error("Error parsing metadata JSON: {}", e.getMessage(), e);
                        // Fallback to simple metadata if parsing fails
                        metadata = Map.of("content", content);
                    }
                    
                    // Create a new Document with the content and parsed metadata
                    return new Document(content, metadata);
                })
                .toList();
        }
        
        @Override
        public void accept(List<Document> documents) {
            add(documents);
        }
        
		/*
		 * @Override public Optional<Boolean> delete(List<String> ids) { if (ids == null
		 * || ids.isEmpty()) { return Optional.of(false); }
		 * 
		 * try { // Convert string IDs to UUIDs and create a list of UUID parameters
		 * List<UUID> uuidList = ids.stream() .map(UUID::fromString) .toList();
		 * 
		 * // Use a parameterized query with UUID array for better security and type
		 * safety Object[] params = uuidList.toArray(); String placeholders =
		 * String.join(",", java.util.Collections.nCopies(params.length, "?"));
		 * 
		 * // Delete documents with the specified IDs int rowsAffected =
		 * jdbcTemplate.update( "DELETE FROM resume_vector_store WHERE resume_id IN (" +
		 * placeholders + ")", params );
		 * 
		 * return Optional.of(rowsAffected > 0); } catch (IllegalArgumentException e) {
		 * // Log error if UUID parsing fails
		 * System.err.println("Error parsing UUID for deletion: " + e.getMessage());
		 * return Optional.of(false); } }
		 */
        
        
        @Override
        public void delete(List<String> ids) {
            if (ids == null || ids.isEmpty()) {
                return;
            }
            
            try {
                // Convert string IDs to UUIDs and create a list of UUID parameters
                List<UUID> uuidList = ids.stream()
                    .map(UUID::fromString)
                    .toList();
                
                // Use a parameterized query with UUID array for better security and type safety
                Object[] params = uuidList.toArray();
                String placeholders = String.join(",", java.util.Collections.nCopies(params.length, "?"));
                
                // Delete documents with the specified IDs
                int rowsAffected = jdbcTemplate.update(
                    "DELETE FROM resume_vector_store WHERE resume_id IN (" + placeholders + ")",
                    params
                );
                
                logger.info("Deleted {} documents from vector store", rowsAffected);
            } catch (IllegalArgumentException e) {
                // Log error if UUID parsing fails
                logger.error("Error parsing UUID for deletion: {}", e.getMessage());
            }
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

		@Override
		public void delete(Expression filterExpression) {
			// TODO Auto-generated method stub
			
		}
		
		// Add this method to your ResumeVectorStore class
		public List<Document> similaritySearchByResumeId(String query, UUID resumeId, int topK) {
		    try {
		        // Generate embedding for the query with retry
		        float[] queryEmbedding = retryTemplate.execute(context -> {
		            try {
		                return embeddingModel.embed(query);
		            } catch (Exception e) {
		                logger.error("Error generating query embedding: {}", e.getMessage());
		                throw e;
		            }
		        }, context -> {
		            logger.error("All retries failed for query embedding generation. Using fallback empty embedding.");
		            return new float[1536]; // Default embedding dimension
		        });
		        
		        // Convert embedding to PostgreSQL vector format
		        String vectorString = convertToVectorString(queryEmbedding);
		        
		        // Perform similarity search with resume_id filter - EFFICIENT!
		        List<Map<String, Object>> results = jdbcTemplate.queryForList(
		            "SELECT id, resume_id, content, metadata, embedding <=> ?::vector AS distance " +
		            "FROM resume_vector_store " +
		            "WHERE resume_id = ? " +  // Filter by resume_id at DB level!
		            "ORDER BY embedding <=> ?::vector " +
		            "LIMIT ?",
		            vectorString,
		            resumeId,      // Resume filter parameter
		            vectorString,
		            topK
		        );
		        
		        logger.info("Found {} relevant sections for resume {}", results.size(), resumeId);
		        
		        // Convert results to Document objects (reuse existing logic)
		        return results.stream()
		            .map(row -> {
		                String content = (String) row.get("content");
		                
		                // Get metadata and handle different types (existing logic)
		                Object metadataObj = row.get("metadata");
		                String metadataStr;
		                
		                if (metadataObj instanceof PGobject) {
		                    metadataStr = ((PGobject) metadataObj).getValue();
		                } else if (metadataObj instanceof String) {
		                    metadataStr = (String) metadataObj;
		                } else if (metadataObj != null) {
		                    metadataStr = metadataObj.toString();
		                    logger.warn("Unexpected metadata type: {}", metadataObj.getClass().getName());
		                } else {
		                    metadataStr = "{}";
		                    logger.warn("Null metadata found in search results");
		                }
		                
		                // Parse metadata JSON back to a Map
		                Map<String, Object> metadata;
		                try {
		                    metadata = objectMapper.readValue(metadataStr, 
		                            objectMapper.getTypeFactory().constructMapType(
		                                    Map.class, String.class, Object.class));
		                } catch (Exception e) {
		                    logger.error("Error parsing metadata JSON: {}", e.getMessage(), e);
		                    metadata = Map.of("content", content);
		                }
		                
		                return new Document(content, metadata);
		            })
		            .toList();
		            
		    } catch (Exception e) {
		        logger.error("Error in resume-specific similarity search: {}", e.getMessage(), e);
		        return List.of();
		    }
		}

    }
}
