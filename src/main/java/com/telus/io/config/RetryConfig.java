package com.telus.io.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for retry mechanism to handle transient errors like 503 Service Unavailable
 */
@Configuration
public class RetryConfig {

    private static final Logger logger = LoggerFactory.getLogger(RetryConfig.class);

    @Value("${resume.matching.ai-retry-count:3}")
    private int maxRetries;

    @Value("${resume.matching.ai-retry-delay-ms:1000}")
    private int initialRetryDelayMs;

    @Value("${resume.matching.ai-retry-max-delay-ms:10000}")
    private int maxRetryDelayMs;

    @Value("${resume.matching.ai-retry-multiplier:2.0}")
    private double backoffMultiplier;

    /**
     * Creates a RetryTemplate for AI service calls with exponential backoff
     * 
     * @return Configured RetryTemplate
     */
    @Bean
    public RetryTemplate aiRetryTemplate() {
        logger.info("Configuring AI retry template with maxRetries={}, initialDelay={}ms", 
                maxRetries, initialRetryDelayMs);
        
        // Define which exceptions should trigger a retry
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(HttpServerErrorException.ServiceUnavailable.class, true); // 503
        retryableExceptions.put(HttpServerErrorException.GatewayTimeout.class, true);     // 504
        retryableExceptions.put(HttpServerErrorException.BadGateway.class, true);         // 502
        retryableExceptions.put(ResourceAccessException.class, true);                     // Network issues
        
        // Create retry policy with the specified exceptions
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                maxRetries,
                retryableExceptions,
                true  // Traverse exception cause hierarchy
        );
        
        // Log the retry policy configuration
        logger.info("Configured retry policy to handle: 503, 504, 502, and network issues");
        
        // Configure exponential backoff
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(initialRetryDelayMs);
        backOffPolicy.setMultiplier(backoffMultiplier);
        backOffPolicy.setMaxInterval(maxRetryDelayMs);
        
        // Create and configure the retry template
        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(retryPolicy);
        template.setBackOffPolicy(backOffPolicy);
        
        return template;
    }
}
