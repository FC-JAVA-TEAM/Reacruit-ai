package com.telus.io.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.telus.io.model.InterviewerVectorStore;


/**
 * Repository for InterviewerVectorStore entities.
 */
@Repository
public interface InterviewerVectorStoreRepository extends JpaRepository<InterviewerVectorStore, UUID> {
    
    /**
     * Find vector store entries by interviewer ID.
     * 
     * @param interviewerId The interviewer ID to search for
     * @return A list of vector store entries for the specified interviewer
     */
    @Query("SELECT v FROM InterviewerVectorStore v WHERE v.interviewer.id = :interviewerId")
    List<InterviewerVectorStore> findByInterviewerId(@Param("interviewerId") UUID interviewerId);
    
    /**
     * Delete vector store entries by interviewer ID.
     * 
     * @param interviewerId The interviewer ID to delete entries for
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM InterviewerVectorStore v WHERE v.interviewer.id = :interviewerId")
    void deleteByInterviewerId(@Param("interviewerId") UUID interviewerId);
    
    /**
     * Perform a similarity search using the PostgreSQL pgvector extension.
     * 
     * @param embedding The embedding vector to search for
     * @param limit The maximum number of results to return
     * @return A list of vector store entries ordered by similarity
     */
    @Query(value = "SELECT * FROM interviewer_vector_store " +
                  "ORDER BY embedding <=> CAST(:embedding AS vector) " +
                  "LIMIT :limit", 
           nativeQuery = true)
    List<InterviewerVectorStore> findSimilar(@Param("embedding") String embedding, @Param("limit") int limit);
    
    /**
     * Check if any interviewer vector store entries exist.
     * 
     * @return True if at least one entry exists, false otherwise
     */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM interviewer_vector_store LIMIT 1)", 
           nativeQuery = true)
    boolean existsAny();
    
    /**
     * Count the total number of interviewer vector store entries.
     * 
     * @return The number of entries
     */
    @Query(value = "SELECT COUNT(*) FROM interviewer_vector_store", 
           nativeQuery = true)
    long countEntries();
    
    /**
     * Perform a similarity search with a metadata filter using the PostgreSQL pgvector extension.
     * 
     * @param embedding The embedding vector to search for
     * @param metadataKey The metadata key to filter on
     * @param metadataValue The metadata value to filter on
     * @param limit The maximum number of results to return
     * @return A list of vector store entries ordered by similarity and filtered by metadata
     */
    @Query(value = "SELECT * FROM interviewer_vector_store " +
                  "WHERE metadata->:metadataKey = CAST(:metadataValue AS jsonb) " +
                  "ORDER BY embedding <=> CAST(:embedding AS vector) " +
                  "LIMIT :limit", 
           nativeQuery = true)
    List<InterviewerVectorStore> findSimilarWithMetadataFilter(
            @Param("embedding") String embedding, 
            @Param("metadataKey") String metadataKey, 
            @Param("metadataValue") String metadataValue, 
            @Param("limit") int limit);
}
