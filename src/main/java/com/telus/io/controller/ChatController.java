package com.telus.io.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telus.io.dto.ChatMessage;
import com.telus.io.dto.ChatRequest_user;
import com.telus.io.dto.ChatResponse;
import com.telus.io.service.ResumeAwareChatService;



/**
 * Controller for chat-related endpoints.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    private final ResumeAwareChatService chatService;
    
    public ChatController(ResumeAwareChatService chatService) {
        this.chatService = chatService;
    }
    
    
    
    
    
    
    
    @PostMapping("/message")
    public ResponseEntity<ChatResponse> chatuser(
            
            @RequestBody ChatRequest_user request) {
        
        logger.info("Received chat message from user {}: {}", request.getCurrentResumeId(), request);
        
        ChatResponse chat_local = chatService.chat_local(request.getCurrentResumeId(), request.getMessage());
        
        return ResponseEntity.ok(chat_local);
    }
   
    /**
     * Get the chat history for a user.
     * 
     * @param userId The ID of the user
     * @return A list of chat messages
     */
    @GetMapping("/{userId}/history")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable String userId) {
        logger.info("Retrieving chat history for user {}", userId);
        
        List<ChatMessage> history = chatService.getChatHistory(userId);
        
        return ResponseEntity.ok(history);
    }
   
}
