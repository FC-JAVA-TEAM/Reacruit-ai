
package com.telus.io.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telus.io.dto.request.LockResumeRequest;
import com.telus.io.exception.CandidateAlreadyLockedException;
import com.telus.io.exception.ResourceNotFoundException;
import com.telus.io.model.CandidateEvaluationModel;
import com.telus.io.model.CandidateStatus;
import com.telus.io.model.Resume;
import com.telus.io.repository.CandidateEvaluationRepository;
import com.telus.io.repository.CandidateStatusHistoryRepository;
import com.telus.io.repository.ResumeRepository;
import com.telus.io.service.CandidateEvaluationService;
import com.telus.io.util.EntityComparisonUtils;
import com.telus.io.util.StatusHistoryUtil;

@Service
public class CandidateEvaluationServiceImpl implements CandidateEvaluationService {

	private static final Logger logger = LoggerFactory.getLogger(CandidateEvaluationServiceImpl.class);

	private final CandidateEvaluationRepository evaluationRepository;
	private final ResumeRepository resumeRepository;
	//private final CandidateStatusHistoryRepository statusHistoryRepository;
	private final StatusHistoryUtil statusHistoryUtil;

	@Autowired
	public CandidateEvaluationServiceImpl(CandidateEvaluationRepository evaluationRepository,
			ResumeRepository resumeRepository, CandidateStatusHistoryRepository statusHistoryRepository,
			StatusHistoryUtil statusHistoryUtil) {
		this.evaluationRepository = evaluationRepository;
		this.resumeRepository = resumeRepository;
	//	this.statusHistoryRepository = statusHistoryRepository;
		this.statusHistoryUtil = statusHistoryUtil;
	}

	@Transactional(readOnly = true)
	public List<CandidateEvaluationModel> findByLocked(boolean locked) {
		return evaluationRepository.findByLocked(locked);
	}

	@Transactional(readOnly = true)
	public boolean isLockedByManager(UUID resumeId, String managerId) {
		return evaluationRepository.findByResumeIdAndLocked(resumeId, true)
				.map(lock -> lock.getManagerId().equals(managerId)).orElse(false);
	}

	@Transactional
	public CandidateEvaluationModel processLockRequest(LockResumeRequest request) {
		logger.debug("Processing lock request for resume ID: {}, manager ID: {}", request.getResumeId(),
				request.getManagerId());

		Resume resume = resumeRepository.findById(request.getResumeId())
				.orElseThrow(() -> new ResourceNotFoundException("Resume", "id", request.getResumeId()));

		Optional<CandidateEvaluationModel> existingLockOpt = evaluationRepository
				.findByResumeIdAndLocked(request.getResumeId(), true);

		if (existingLockOpt.isPresent() && !existingLockOpt.get().getManagerId().equals(request.getManagerId())) {
			throw new CandidateAlreadyLockedException(request.getResumeId(), existingLockOpt.get().getManagerId());
		}

		Optional<CandidateEvaluationModel> existingEvaluationOpt = evaluationRepository
				.findByResumeId(request.getResumeId());

		if (existingEvaluationOpt.isPresent()
				&& isRequestIdenticalToExistingEvaluation(request, existingEvaluationOpt.get())) {
			logger.debug("No changes detected. Skipping update.");
			return existingEvaluationOpt.get();
		}

		CandidateEvaluationModel evaluation = existingEvaluationOpt.orElse(new CandidateEvaluationModel());
		boolean isNew = evaluation.getId() == null;

		if (isNew) {
			evaluation.setResumeId(request.getResumeId());
			evaluation.setName(resume.getName());
			evaluation.setEmail(resume.getEmail());
			evaluation.setPhoneNumber(resume.getPhoneNumber());
		}

		applyRequestToEvaluation(request, evaluation);

		if (!updateEvaluationIfChanged(evaluation)) {
			logger.debug("No actual changes to save after diff.");
			return evaluation;
		}

		CandidateEvaluationModel saved = evaluationRepository.save(evaluation);

		statusHistoryUtil.recordStatusChange(request.getResumeId(), saved.getId(), null, // previousStatus
				null, // previousCustomStatus
				saved.getStatus(), saved.getCustomStatus(), saved.getManagerId(),
				isNew ? "New candidate initialized" : null);

		return saved;
	}

	private void applyRequestToEvaluation(LockResumeRequest request, CandidateEvaluationModel evaluation) {
		evaluation.setExecutiveSummary(request.getExecutiveSummary());
		evaluation.setKeyStrengths(request.getKeyStrengths());
		evaluation.setImprovementAreas(request.getImprovementAreas());

		evaluation.setScore(request.getScore());
		evaluation.setLocked(request.isLocked());
		evaluation.setManagerId(request.getManagerId());
		evaluation.setLockedAt(LocalDateTime.now());

		evaluation.setRecommendationType(request.getRecommendationType());
		evaluation.setRecommendationReason(request.getRecommendationReason());

		tryParseInt(request.getTechnicalSkills()).ifPresent(evaluation::setTechnicalSkills);
		tryParseInt(request.getExperience()).ifPresent(evaluation::setExperience);
		tryParseInt(request.getEducation()).ifPresent(evaluation::setEducation);
		tryParseInt(request.getSoftSkills()).ifPresent(evaluation::setSoftSkills);
		tryParseInt(request.getAchievements()).ifPresent(evaluation::setAchievements);

		if (request.getStatus() != null) {
			try {
				evaluation.setStatus(CandidateStatus.valueOf(request.getStatus()));
			} catch (IllegalArgumentException e) {
				logger.warn("Invalid status: {}", request.getStatus());
			}
		}
	}

	private Optional<Integer> tryParseInt(String value) {
		try {
			return Optional.ofNullable(value).map(Integer::parseInt);
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	private boolean updateEvaluationIfChanged(CandidateEvaluationModel newEval) {
		if (newEval.getId() == null)
			return true;

		CandidateEvaluationModel existing = evaluationRepository.findById(newEval.getId()).orElse(null);
		if (existing == null)
			return true;

		if (!hasEvaluationDataChanged(existing, newEval))
			return false;

		EntityComparisonUtils.handleCollectionChanges(existing.getKeyStrengths(), newEval.getKeyStrengths());
		EntityComparisonUtils.handleCollectionChanges(existing.getImprovementAreas(), newEval.getImprovementAreas());

		copyNonNullProperties(newEval, existing);
		return true;
	}

	private boolean hasEvaluationDataChanged(CandidateEvaluationModel oldEval, CandidateEvaluationModel newEval) {
		return !Objects.equals(oldEval.getExecutiveSummary(), newEval.getExecutiveSummary())
				|| !Objects.equals(oldEval.getScore(), newEval.getScore())
				|| !EntityComparisonUtils.areCollectionsEqual(oldEval.getKeyStrengths(), newEval.getKeyStrengths())
				|| !EntityComparisonUtils.areCollectionsEqual(oldEval.getImprovementAreas(),
						newEval.getImprovementAreas());
	}

	private void copyNonNullProperties(CandidateEvaluationModel src, CandidateEvaluationModel target) {
		if (src.getExecutiveSummary() != null)
			target.setExecutiveSummary(src.getExecutiveSummary());
		if (src.getScore() != null)
			target.setScore(src.getScore());
		if (src.getTechnicalSkills() != null)
			target.setTechnicalSkills(src.getTechnicalSkills());
		if (src.getExperience() != null)
			target.setExperience(src.getExperience());
		if (src.getEducation() != null)
			target.setEducation(src.getEducation());
		if (src.getSoftSkills() != null)
			target.setSoftSkills(src.getSoftSkills());
		if (src.getAchievements() != null)
			target.setAchievements(src.getAchievements());
		if (src.getRecommendationType() != null)
			target.setRecommendationType(src.getRecommendationType());
		if (src.getRecommendationReason() != null)
			target.setRecommendationReason(src.getRecommendationReason());
		if (src.getStatus() != null)
			target.setStatus(src.getStatus());
		if (src.getCustomStatus() != null)
			target.setCustomStatus(src.getCustomStatus());
		target.setLocked(src.isLocked());
		target.setLockedAt(src.getLockedAt());
		target.setManagerId(src.getManagerId());
	}

	private boolean isRequestIdenticalToExistingEvaluation(LockResumeRequest request,
			CandidateEvaluationModel existing) {
		if (existing.isLocked() != request.isLocked())
			return false;
		if (!Objects.equals(existing.getManagerId(), request.getManagerId()))
			return false;

		if (!request.hasEvaluationData())
			return true;

		return Objects.equals(existing.getExecutiveSummary(), request.getExecutiveSummary())
				&& EntityComparisonUtils.areCollectionsEqual(existing.getKeyStrengths(), request.getKeyStrengths())
				&& EntityComparisonUtils.areCollectionsEqual(existing.getImprovementAreas(),
						request.getImprovementAreas())
				&& Objects.equals(existing.getScore(), request.getScore());
	}

	@Override
	public Optional<CandidateEvaluationModel> findByResumeId(UUID resumeId) {
		return evaluationRepository.findByResumeId(resumeId);
	}
}
