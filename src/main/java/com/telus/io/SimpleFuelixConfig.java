package com.telus.io;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SimpleFuelixConfig {
    
    @Bean
    @Primary
    public OpenAiApi openAiApi() {
        return OpenAiApi.builder()
            .baseUrl("${spring.ai.openai.base-url}")
            .apiKey("${spring.ai.openai.api-key}")
            .build();
    }
    
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
