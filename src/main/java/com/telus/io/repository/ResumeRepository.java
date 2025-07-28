package com.telus.io.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.telus.io.model.Resume;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Resume entities.
 */
@Repository
public interface ResumeRepository extends JpaRepository<Resume, UUID> {
    
    /**
     * Find a resume by name, email, and phone number.
     * 
     * @param name The name to search for
     * @param email The email to search for
     * @param phoneNumber The phone number to search for
     * @return The resume, if found
     */
    Optional<Resume> findByNameAndEmailAndPhoneNumber(String name, String email, String phoneNumber);
    
    /**
     * Get all resume IDs.
     * 
     * @return A list of all resume IDs
     */
    @Query("SELECT r.id FROM Resume r")
    List<UUID> findAllIds();
}
