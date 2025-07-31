package com.telus.io.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for asynchronous execution.
 * Defines thread pools for different types of operations.
 * Uses externalized configuration from application.properties.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Value("${app.async.resume-processing.core-pool-size:20}")
    private int resumeProcessingCorePoolSize;
    
    @Value("${app.async.resume-processing.max-pool-size:40}")
    private int resumeProcessingMaxPoolSize;
    
    @Value("${app.async.resume-processing.queue-capacity:100}")
    private int resumeProcessingQueueCapacity;
    
    @Value("${app.async.ai-operations.core-pool-size:30}")
    private int aiOperationsCorePoolSize;
    
    @Value("${app.async.ai-operations.max-pool-size:60}")
    private int aiOperationsMaxPoolSize;
    
    @Value("${app.async.ai-operations.queue-capacity:200}")
    private int aiOperationsQueueCapacity;
    
    /**
     * Executor for general resume processing operations.
     * 
     * @return The configured executor
     */
    @Bean(name = "resumeProcessingExecutor")
    public Executor resumeProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(resumeProcessingCorePoolSize);
        executor.setMaxPoolSize(resumeProcessingMaxPoolSize);
        executor.setQueueCapacity(resumeProcessingQueueCapacity);
        executor.setThreadNamePrefix("ResumeProc-");
        executor.initialize();
        return executor;
    }
    
    /**
     * Executor specifically for AI operations, which may be more resource-intensive.
     * 
     * @return The configured executor
     */
    @Bean(name = "aiOperationsExecutor")
    public Executor aiOperationsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(aiOperationsCorePoolSize);
        executor.setMaxPoolSize(aiOperationsMaxPoolSize);
        executor.setQueueCapacity(aiOperationsQueueCapacity);
        executor.setThreadNamePrefix("AI-Op-");
        // Use CallerRunsPolicy to prevent rejection when queue is full
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
    
    /**
     * General task executor for other async operations.
     * 
     * @return The configured executor
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("Task-");
        executor.initialize();
        return executor;
    }
}
