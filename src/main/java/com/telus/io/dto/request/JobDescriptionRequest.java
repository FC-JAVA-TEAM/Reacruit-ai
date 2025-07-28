package com.telus.io.dto.request;

public class JobDescriptionRequest {

	private String jobDescription;
	private int count = 10; // Default to 5 candidates if not specified

	// Getters and setters
	public String getJobDescription() {
		return jobDescription;
	}

	public void setJobDescription(String jobDescription) {
		this.jobDescription = jobDescription;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		// Ensure count is between 1 and 10 to prevent abuse
		this.count = Math.min(Math.max(count, 1), 10);
	}
}
