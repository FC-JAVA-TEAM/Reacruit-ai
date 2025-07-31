package com.telus.io.config;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.CorsRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//@Configuration
//public class WebConfig implements WebMvcConfigurer {
//
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**")
//                .allowedOrigins("*")  // In production, specify exact origins like "http://localhost:3000"
//                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
//                .allowedHeaders("*")
//                .exposedHeaders("Authorization")
//                .allowCredentials(true)
//                .maxAge(3600);
//    }
//}



//package com.telus.spring.ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*") // ✅ use this instead of allowedOrigins("*") for wildcard support with credentials
                .allowedMethods("*")        // GET, POST, PUT, DELETE, etc.
                .allowedHeaders("*")        // Allow all headers
               // .exposedHeaders("Authorization") // Optional: expose custom headers
                .allowCredentials(true)     // ✅ allow cookies/auth headers
                .maxAge(3600);              // Cache preflight response for 1 hour
    }
}

