package com.telus.io.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a synchronization operation is requested while another sync is in progress.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class SyncInProgressException extends RuntimeException {
    
    public SyncInProgressException(String message) {
        super(message);
    }
}
