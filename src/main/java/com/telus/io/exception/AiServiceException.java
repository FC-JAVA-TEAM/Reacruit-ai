package com.telus.io.exception;

/**
 * Exception thrown when there is an issue with the AI service.
 * This is a base class for more specific AI service exceptions.
 */
public class AiServiceException extends RuntimeException {
    
    public AiServiceException(String message) {
        super(message);
    }
    
    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
