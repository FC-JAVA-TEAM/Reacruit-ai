package com.telus.io.dto.request;

public class PromptRequest {

	private String systemPrompt;
	private String userPrompt;

	// Getters and setters
	public String getSystemPrompt() {
		return systemPrompt;
	}

	public void setSystemPrompt(String systemPrompt) {
		this.systemPrompt = systemPrompt;
	}

	public String getUserPrompt() {
		return userPrompt;
	}

	public void setUserPrompt(String userPrompt) {
		this.userPrompt = userPrompt;
	}
}