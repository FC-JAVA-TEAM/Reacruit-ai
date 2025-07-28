package com.telus.io.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when the AI service request times out.
 * This happens when the service takes too long to respond.
 */
@ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
public class AiServiceTimeoutException extends AiServiceException {
    
    public AiServiceTimeoutException(String message) {
        super(message);
    }
    
    public AiServiceTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
