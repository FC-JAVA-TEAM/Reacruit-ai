package com.telus.io.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telus.io.dto.request.JobDescriptionRequest;
import com.telus.io.dto.request.PromptRequest;
import com.telus.io.model.Resume;
import com.telus.io.model.ResumeAnalysis;
import com.telus.io.model.ResumeMatch;

/**
 * Controller for AI-powered candidate generation and matching. This controller
 * provides a simplified interface for interacting with AI models and directly
 * mapping responses to domain objects.
 */
@RestController
@RequestMapping("/api/candidates")
public class FuelixCandidateController {

	private static final Logger logger = LoggerFactory.getLogger(FuelixCandidateController.class);

	private final ChatClient chatClient;

	public FuelixCandidateController(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	/**
	 * Generate a candidate profile based on a prompt.
	 * 
	 * @param prompt The prompt to send to the AI model
	 * @return A ResumeMatch object containing the generated candidate profile
	 */
	@PostMapping("/generate")
	public ResponseEntity<ResumeMatch> generateCandidate(@RequestBody String prompt) {
		logger.info("Generating candidate profile with prompt: {}", prompt);

		try {
			ResumeMatch candidateMatch = chatClient.prompt().user(prompt).call().entity(ResumeMatch.class);

			logger.info("Successfully generated candidate profile");
			return ResponseEntity.ok(candidateMatch);
		} catch (Exception e) {
			logger.error("Error generating candidate profile", e);
			throw new RuntimeException("Failed to generate candidate profile: " + e.getMessage(), e);
		}
	}

	@PostMapping("/generate-candidates")
	public ResponseEntity<List<ResumeMatch>> generateCandidatesForJD(@RequestBody JobDescriptionRequest request) {

		logger.info("Generating {} candidate profiles for job description", request.getCount());

		// Create a system prompt with very explicit instructions
		String systemPrompt = "You are a resume generator that creates fictional candidate profiles tailored to match a job description. "
				+ "Generate EXACTLY " + request.getCount()
				+ " candidates with varying levels of qualification (some excellent matches, some good matches, and some average matches). "
				+ "Your response must follow this exact format for each candidate:\n\n" + "CANDIDATE START\n"
				+ "NAME: [candidate name]\n" + "EMAIL: [email]\n" + "PHONE: [phone number]\n"
				+ "SUMMARY: [brief professional summary that relates to the job description]\n"
				+ "SKILLS: [comma-separated list of skills relevant to the job description]\n"
				+ "EXPERIENCE: [brief experience summary highlighting relevance to the job description]\n"
				+ "CANDIDATE END\n\n"
				+ "Repeat this format for each candidate. Do not include any other text or formatting.";

		try {
			// Get raw text response from AI
			String rawResponse = chatClient.prompt().system(systemPrompt)
					.user("Job Description: " + request.getJobDescription()).call().content();

			// Manually parse the response into ResumeMatch objects
			List<ResumeMatch> candidateMatches = parseRawResponseToResumeMatches(rawResponse);

			logger.info("Successfully generated {} candidate profiles", candidateMatches.size());
			return ResponseEntity.ok(candidateMatches);
		} catch (Exception e) {
			logger.error("Error generating candidate profiles", e);
			throw new RuntimeException("Failed to generate candidate profiles: " + e.getMessage(), e);
		}
	}

	// Request class for the new endpoint

	private List<ResumeMatch> parseRawResponseToResumeMatches(String rawResponse) {
		List<ResumeMatch> results = new ArrayList<>();

		// Split the response by "CANDIDATE START" to get individual candidate blocks
		String[] candidateBlocks = rawResponse.split("CANDIDATE START");

		for (String block : candidateBlocks) {
			if (block.trim().isEmpty())
				continue;

			try {
				// Extract fields using regex or string operations
				String name = extractField(block, "NAME:", "EMAIL:");
				String email = extractField(block, "EMAIL:", "PHONE:");
				String phone = extractField(block, "PHONE:", "SUMMARY:");
				String summary = extractField(block, "SUMMARY:", "SKILLS:");
				String skills = extractField(block, "SKILLS:", "EXPERIENCE:");
				String experience = extractField(block, "EXPERIENCE:", "CANDIDATE END");

				// Create Resume object
				Resume resume = new Resume();
				resume.setId(UUID.randomUUID());
				resume.setName(name);
				resume.setEmail(email);
				resume.setPhoneNumber(phone);
				resume.setFullText(summary + "\n\nSkills: " + skills + "\n\nExperience: " + experience);
				resume.setFileType("AI-generated");
				resume.setOriginalFileName(name.replace(" ", "_") + "_resume.pdf");
				resume.setUploadedAt(LocalDateTime.now());
				resume.setUpdatedAt(LocalDateTime.now());
				resume.setLocked(false); // Set locked to false by default

				// Create ResumeAnalysis object with all fields populated
				ResumeAnalysis analysis = new ResumeAnalysis();
				analysis.setExecutiveSummary(summary);

				// Generate a random score between 70-95
				int score = new Random().nextInt(26) + 70;
				analysis.setOverallScore(score);

				// Create key strengths
				List<ResumeAnalysis.KeyStrength> keyStrengths = new ArrayList<>();
				String[] skillsArray = skills.split(",");
				for (int i = 0; i < Math.min(3, skillsArray.length); i++) {
					ResumeAnalysis.KeyStrength strength = new ResumeAnalysis.KeyStrength();
					strength.setStrength(skillsArray[i].trim());
					strength.setEvidence("Demonstrated in professional experience");
					keyStrengths.add(strength);
				}
				analysis.setKeyStrengths(keyStrengths);

				// Create improvement areas
				List<ResumeAnalysis.ImprovementArea> improvementAreas = new ArrayList<>();
				ResumeAnalysis.ImprovementArea area = new ResumeAnalysis.ImprovementArea();
				area.setGap("Could benefit from more specific achievements");
				area.setSuggestion("Add quantifiable results to demonstrate impact");
				improvementAreas.add(area);
				analysis.setImprovementAreas(improvementAreas);

				// Create category scores
				ResumeAnalysis.CategoryScores categoryScores = new ResumeAnalysis.CategoryScores();
				categoryScores.setTechnicalSkills(new Random().nextInt(21) + 70);
				categoryScores.setExperience(new Random().nextInt(21) + 70);
				categoryScores.setEducation(new Random().nextInt(21) + 70);
				categoryScores.setSoftSkills(new Random().nextInt(21) + 70);
				categoryScores.setAchievements(new Random().nextInt(21) + 70);
				analysis.setCategoryScores(categoryScores);

				// Create skill explanations
				Map<String, String> skillExplanations = new HashMap<>();
				for (int i = 0; i < Math.min(3, skillsArray.length); i++) {
					skillExplanations.put(skillsArray[i].trim(),
							"Demonstrated proficiency in " + skillsArray[i].trim());
				}
				analysis.setSkillExplanations(skillExplanations);

				// Create recommendation
				ResumeAnalysis.Recommendation recommendation = new ResumeAnalysis.Recommendation();
				recommendation.setType(score > 85 ? "Strong Match" : "Potential Match");
				recommendation.setReason(score > 85 ? "Candidate has strong technical skills and relevant experience"
						: "Candidate has relevant skills but may need additional training");
				analysis.setRecommendation(recommendation);

				// Create ResumeMatch object
				ResumeMatch match = new ResumeMatch();
				match.setResume(resume);
				match.setAnalysis(analysis);
				match.setScore(score);
				match.setExplanation("Candidate has " + experience.split(" ")[0] + " of experience in "
						+ skills.split(",")[0] + " and related technologies.");
				match.setLocked(false);
				match.setManagerId(null); // No manager assigned by default

				results.add(match);
			} catch (Exception e) {
				logger.warn("Failed to parse candidate block: {}", block, e);
				// Continue with next candidate
			}
		}

		return results;
	}

	private String extractField(String text, String startMarker, String endMarker) {
		int startIndex = text.indexOf(startMarker) + startMarker.length();
		int endIndex = text.indexOf(endMarker);
		if (startIndex >= 0 && endIndex >= 0 && startIndex < endIndex) {
			return text.substring(startIndex, endIndex).trim();
		}
		return "";
	}

	/**
	 * Generate a candidate profile with system context.
	 * 
	 * @param request The request containing system and user prompts
	 * @return A ResumeMatch object containing the generated candidate profile
	 */
	@PostMapping("/generate-with-context")
	public ResponseEntity<ResumeMatch> generateCandidateWithContext(@RequestBody PromptRequest request) {
		logger.info("Generating candidate profile with context. System: {}, User: {}", request.getSystemPrompt(),
				request.getUserPrompt());

		try {
			ResumeMatch candidateMatch = chatClient.prompt().system(request.getSystemPrompt())
					.user(request.getUserPrompt()).call().entity(ResumeMatch.class);

			logger.info("Successfully generated candidate profile with context");
			return ResponseEntity.ok(candidateMatch);
		} catch (Exception e) {
			logger.error("Error generating candidate profile with context", e);
			throw new RuntimeException("Failed to generate candidate profile with context: " + e.getMessage(), e);
		}
	}

}
