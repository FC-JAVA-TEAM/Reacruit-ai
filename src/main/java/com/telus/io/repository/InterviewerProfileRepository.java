package com.telus.io.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.telus.io.model.InterviewerProfile;


/**
 * Repository for InterviewerProfile entities.
 */
@Repository
public interface InterviewerProfileRepository extends JpaRepository<InterviewerProfile, UUID> {
    
    /**
     * Find an interviewer by email.
     * 
     * @param email The email to search for
     * @return The interviewer profile, if found
     */
    Optional<InterviewerProfile> findByEmail(String email);
    
    /**
     * Find interviewers by technical expertise.
     * 
     * @param expertise The technical expertise to search for
     * @return A list of interviewer profiles with the specified expertise
     */
    @Query(value = "SELECT * FROM interviewer_profiles i WHERE i.technical_expertise @> CAST(:expertise AS jsonb)", nativeQuery = true)
    List<InterviewerProfile> findByTechnicalExpertise(@Param("expertise") String expertise);
    
    /**
     * Find interviewers by specialization.
     * 
     * @param specialization The specialization to search for
     * @return A list of interviewer profiles with the specified specialization
     */
    @Query(value = "SELECT * FROM interviewer_profiles i WHERE i.specializations @> CAST(:specialization AS jsonb)", nativeQuery = true)
    List<InterviewerProfile> findBySpecialization(@Param("specialization") String specialization);
    
    /**
     * Find interviewers by tier.
     * 
     * @param tier The interviewer tier to search for
     * @return A list of interviewer profiles with the specified tier
     */
    List<InterviewerProfile> findByInterviewerTier(int tier);
    
    /**
     * Find interviewers by minimum experience years.
     * 
     * @param years The minimum experience years to search for
     * @return A list of interviewer profiles with at least the specified years of experience
     */
    @Query("SELECT i FROM InterviewerProfile i WHERE i.experienceYears >= :years")
    List<InterviewerProfile> findByMinimumExperienceYears(@Param("years") int years);
    
    /**
     * Get all interviewer IDs.
     * 
     * @return A list of all interviewer IDs
     */
    @Query("SELECT i.id FROM InterviewerProfile i")
    List<UUID> findAllIds();
}
