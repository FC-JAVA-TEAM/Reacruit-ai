package com.telus.io.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when the AI service is unavailable.
 * This typically happens when the service returns a 503 error or is unreachable.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class AiServiceUnavailableException extends AiServiceException {
    
    public AiServiceUnavailableException(String message) {
        super(message);
    }
    
    public AiServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
