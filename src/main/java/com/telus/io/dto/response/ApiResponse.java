package com.telus.io.dto.response;

import java.io.Serializable;

/**
 * Generic API response model for REST endpoints.
 */
public class ApiResponse implements Serializable {
    
    private boolean success;
    private String message;
    private Object data;
    
    public ApiResponse() {
    }
    
    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public ApiResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
}
