package com.telus.io.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Data Transfer Object for candidate evaluation information.
 * Used for displaying and transferring candidate evaluation data.
 * Also serves as an entity for database operations.
 */
@Entity
@Table(name = "candidate_evaluations")
public class CandidateEvaluationModel {
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        CandidateEvaluationModel that = (CandidateEvaluationModel) o;
        
        if (score != that.score) return false;
        if (locked != that.locked) return false;
        if (!Objects.equals(resumeId, that.resumeId)) return false;
        if (!Objects.equals(name, that.name)) return false;
        if (!Objects.equals(email, that.email)) return false;
        if (!Objects.equals(phoneNumber, that.phoneNumber)) return false;
        if (!Objects.equals(executiveSummary, that.executiveSummary)) return false;
        if (!Objects.equals(keyStrengths, that.keyStrengths)) return false;
        if (!Objects.equals(improvementAreas, that.improvementAreas)) return false;
        if (!Objects.equals(technicalSkills, that.technicalSkills)) return false;
        if (!Objects.equals(experience, that.experience)) return false;
        if (!Objects.equals(education, that.education)) return false;
        if (!Objects.equals(softSkills, that.softSkills)) return false;
        if (!Objects.equals(achievements, that.achievements)) return false;
        if (!Objects.equals(recommendationType, that.recommendationType)) return false;
        if (!Objects.equals(recommendationReason, that.recommendationReason)) return false;
        if (status != that.status) return false;
        if (!Objects.equals(customStatus, that.customStatus)) return false;
        if (!Objects.equals(managerId, that.managerId)) return false;
        return Objects.equals(lockedAt, that.lockedAt);
    }
    
    @Override
    public int hashCode() {
        int result = resumeId != null ? resumeId.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (phoneNumber != null ? phoneNumber.hashCode() : 0);
        result = 31 * result + score;
        result = 31 * result + (executiveSummary != null ? executiveSummary.hashCode() : 0);
        result = 31 * result + (keyStrengths != null ? keyStrengths.hashCode() : 0);
        result = 31 * result + (improvementAreas != null ? improvementAreas.hashCode() : 0);
        result = 31 * result + (technicalSkills != null ? technicalSkills.hashCode() : 0);
        result = 31 * result + (experience != null ? experience.hashCode() : 0);
        result = 31 * result + (education != null ? education.hashCode() : 0);
        result = 31 * result + (softSkills != null ? softSkills.hashCode() : 0);
        result = 31 * result + (achievements != null ? achievements.hashCode() : 0);
        result = 31 * result + (recommendationType != null ? recommendationType.hashCode() : 0);
        result = 31 * result + (recommendationReason != null ? recommendationReason.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (customStatus != null ? customStatus.hashCode() : 0);
        result = 31 * result + (locked ? 1 : 0);
        result = 31 * result + (managerId != null ? managerId.hashCode() : 0);
        result = 31 * result + (lockedAt != null ? lockedAt.hashCode() : 0);
        return result;
    }
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
	@Column(name = "resume_Id")
    private UUID resumeId;	
    
    private String name;
    private String email;
    private String phoneNumber;
    private int score;
    private String executiveSummary;
    
    @ElementCollection
    @CollectionTable(name = "candidate_evaluation_model_key_strengths", 
                    joinColumns = @JoinColumn(name = "candidate_evaluation_model_id"))
    @Column(name = "key_strengths")
    private List<String> keyStrengths;
    
    @ElementCollection
    @CollectionTable(name = "candidate_evaluation_model_improvement_areas", 
                    joinColumns = @JoinColumn(name = "candidate_evaluation_model_id"))
    @Column(name = "improvement_areas")
    private List<String> improvementAreas;
    private  Integer technicalSkills;
    private Integer experience;
    private  Integer education;
    private  Integer softSkills;
    private  Integer achievements;
    private String recommendationType;
    private String recommendationReason;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CandidateStatus status = CandidateStatus.OPEN; // Default status
    
    @Column(name = "custom_status")
    private String customStatus;
    
    // For backward compatibility
   // @Deprecated
    private boolean locked;
    
    // Added for lock functionality
    private String managerId;
    private LocalDateTime lockedAt;
    
    // For optimistic locking
//    @Version
//    private Long version;
    
    // Default constructor
    public CandidateEvaluationModel() {
    }
    
    // Getters and setters for id
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    // Getters and setters
    public UUID getResumeId() {
        return resumeId;
    }
    
    public void setResumeId(UUID resumeId) {
        this.resumeId = resumeId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public  Integer getScore() {
        return score;
    }
    
    public void setScore( Integer score) {
        this.score = score;
    }
    
    public String getExecutiveSummary() {
        return executiveSummary;
    }
    
    public void setExecutiveSummary(String executiveSummary) {
        this.executiveSummary = executiveSummary;
    }
    
    public List<String> getKeyStrengths() {
        return keyStrengths;
    }
    
    public void setKeyStrengths(List<String> keyStrengths) {
        this.keyStrengths = keyStrengths;
    }
    
    public List<String> getImprovementAreas() {
        return improvementAreas;
    }
    
    public void setImprovementAreas(List<String> improvementAreas) {
        this.improvementAreas = improvementAreas;
    }
    
    public  Integer getTechnicalSkills() {
        return technicalSkills;
    }
    
    public void setTechnicalSkills( Integer technicalSkills) {
        this.technicalSkills = technicalSkills;
    }
    
    public  Integer getExperience() {
        return experience;
    }
    
    public void setExperience( Integer experience) {
        this.experience = experience;
    }
    
    public  Integer getEducation() {
        return education;
    }
    
    public void setEducation( Integer education) {
        this.education = education;
    }
    
    public  Integer getSoftSkills() {
        return softSkills;
    }
    
    public void setSoftSkills( Integer softSkills) {
        this.softSkills = softSkills;
    }
    
    public  Integer getAchievements() {
        return achievements;
    }
    
    public void setAchievements(int achievements) {
        this.achievements = achievements;
    }
    
    public String getRecommendationType() {
        return recommendationType;
    }
    
    public void setRecommendationType(String recommendationType) {
        this.recommendationType = recommendationType;
    }
    
    public String getRecommendationReason() {
        return recommendationReason;
    }
    
    public void setRecommendationReason(String recommendationReason) {
        this.recommendationReason = recommendationReason;
    }
    
    public boolean isLocked() {
        return locked ;
    }
    
    public void setLocked(boolean locked) {
        this.locked = locked;
        // Update status for consistency
//        if (locked && status != CandidateStatus.LOCKED) {
//            this.status = CandidateStatus.LOCKED;
//        } else if (!locked && status == CandidateStatus.LOCKED) {
//            this.status = CandidateStatus.OPEN;
//        }
    }
    
    public CandidateStatus getStatus() {
        return status;
    }
    
    public void setStatus(CandidateStatus status) {
        this.status = status;
        // Update locked flag for backward compatibility
       // this.locked = (status == CandidateStatus.LOCKED);
    }
    
    public String getCustomStatus() {
        return customStatus;
    }
    
    public void setCustomStatus(String customStatus) {
        this.customStatus = customStatus;
    }
    
    public String getManagerId() {
        return managerId;
    }
    
    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }
    
    public LocalDateTime getLockedAt() {
        return lockedAt;
    }
    
    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }
    
//    public Long getVersion() {
//        return version;
//    }
//    
//    public void setVersion(Long version) {
//        this.version = version;
//    }
}
