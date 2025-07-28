package com.telus.io.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.telus.io.config.ResumeVectorStoreConfig;
import com.telus.io.dto.ChatMessage;
import com.telus.io.dto.ChatResponse;
import com.telus.io.repository.ResumeRepository;

/**
 * Service for handling chat interactions with resume context awareness.
 * Integrates with the resume matching functionality.
 */
@Service
public class ResumeAwareChatService {

	private static final Logger logger = LoggerFactory.getLogger(ResumeAwareChatService.class);

	private final ChatClient chatClient;
	// private final ResumeRepository resumeRepository;
	private final ChatMemory chatMemory;

	private final VectorStore resumeVectorStore;

	public ResumeAwareChatService(ChatClient chatClient, ResumeRepository resumeRepository,
			@Qualifier("resumeVectorStore") VectorStore resumeVectorStore, // ADD THIS!
			ChatMemory chatMemory) {
		this.chatClient = chatClient;
		// this.resumeRepository = resumeRepository;
		this.chatMemory = chatMemory;
		this.resumeVectorStore = resumeVectorStore; // ADD THIS!
	}

	public ChatResponse chat_local(UUID currentResumeId, String message) {
		logger.info("Processing chat message from user {}: {}", currentResumeId, message);

		// Check for special commands

		// Create system prompt with context
		String systemPrompt = createSystemPrompt_CHAT(currentResumeId, message);

		// Regular chat - use ChatClient with memory
		String response = chatClient.prompt().system(systemPrompt).user(message)
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, currentResumeId)).call().content();

		logger.info("Generated response for user {}: {}", currentResumeId, response);
		return new ChatResponse(response);
	}

	// Update this method
	private String createSystemPrompt_CHAT(UUID resumeId, String message) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("You are an AI assistant specializing in resume analysis and job matching. ");

		if (resumeId != null) {
			// Use the new resume-specific search method
			List<Document> relevantSections = getRelevantResumeContext(resumeId, message);

			if (!relevantSections.isEmpty()) {
				prompt.append("Here are the relevant sections from the resume: ");
				for (Document doc : relevantSections) {
					prompt.append("\"").append(doc.getText()).append("\" ");
				}
			} else {
				prompt.append("No specific resume context found for this query. ");
			}
		}

		prompt.append("Provide helpful, accurate information about resume matching. ");
		prompt.append("When asked about match quality, explain the factors that contribute to a good match. ");
		prompt.append("You can use special commands like /explain, /refine, and /compare for specific functionality. ");
		return prompt.toString();
	}

	// Simple method that uses the vector store's new capability
	private List<Document> getRelevantResumeContext(UUID resumeId, String message) {
		try {
			// Cast to your custom implementation and use the new method
			ResumeVectorStoreConfig.ResumeVectorStore customStore = (ResumeVectorStoreConfig.ResumeVectorStore) resumeVectorStore;

			return customStore.similaritySearchByResumeId(message, resumeId, 3);

		} catch (Exception e) {
			logger.error("Error in vector search for resume {}: {}", resumeId, e.getMessage(), e);
			return List.of();
		}
	}

	/**
	 * Get the chat history for a user.
	 * 
	 * @param userId The ID of the user
	 * @return A list of chat messages
	 */
	public List<ChatMessage> getChatHistory(String userId) {
		List<ChatMessage> history = new ArrayList<>();

		// Get messages from chat memory
		List<Message> messages = chatMemory.get(userId);

		// Convert to ChatMessage objects
		for (Message message : messages) {
			String role = message.getMessageType().toString().toLowerCase();
			String content = message.toString();
			history.add(new ChatMessage(role, content));
		}

		return history;
	}

}
