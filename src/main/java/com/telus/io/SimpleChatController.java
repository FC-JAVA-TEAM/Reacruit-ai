package com.telus.io;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//SimpleChatController.java
@RestController
@RequestMapping("/api/simple-chat")
public class SimpleChatController {
 
 private final ChatModel chatModel; // Auto-injected by Spring AI
 
 public SimpleChatController(ChatModel chatModel) {
     this.chatModel = chatModel;
 }
 
 @PostMapping("/ask")
 public String askQuestion(@RequestBody String question) {
     // Direct chat with Fuelix
     ChatResponse response = chatModel.call(
         new Prompt(question)
     );
     return response.getResult().getOutput().getText();
 }
 
 @PostMapping("/analyze-resume")
 public String analyzeResume(@RequestBody String resumeText) {
     String prompt = "Analyze this resume and provide key skills: " + resumeText;
     return chatModel.call(new Prompt(prompt))
                   .getResult()
                   .getOutput()
                   .getText();
 }
}
