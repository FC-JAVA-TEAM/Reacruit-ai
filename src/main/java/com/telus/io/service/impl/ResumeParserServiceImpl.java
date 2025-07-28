package com.telus.io.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telus.io.dto.ResumeParseResult;
import com.telus.io.service.ResumeParserService;

/**
 * Implementation of ResumeParserService that handles different file types
 * and uses AI to extract structured information.
 * Supports both synchronous and asynchronous processing.
 */
@Service
public class ResumeParserServiceImpl implements ResumeParserService {
    
    private static final Logger logger = LoggerFactory.getLogger(ResumeParserServiceImpl.class);
    
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    
    @Autowired
    @Qualifier("resumeProcessingExecutor")
    private Executor resumeProcessingExecutor;
    
    public ResumeParserServiceImpl(ChatClient.Builder builder, ObjectMapper objectMapper) {
        this.chatClient = builder.build();
        this.objectMapper = objectMapper;
    }
    
    @Override
    public ResumeParseResult parseResume(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String fileType = getFileType(originalFilename);
        String content = extractText(file, fileType);
        
        // Extract structured information using AI
        Map<String, String> extractedInfo = extractInformation(content);
        
        return new ResumeParseResult(
                extractedInfo.get("name"),
                extractedInfo.get("email"),
                extractedInfo.get("phoneNumber"),
                content,
                fileType
        );
    }
    
    @Override
    @Async("resumeProcessingExecutor")
    public CompletableFuture<ResumeParseResult> parseResumeAsync(MultipartFile file) {
        try {
            logger.info("Processing resume asynchronously: {}", file.getOriginalFilename());
            ResumeParseResult result = parseResume(file);
            return CompletableFuture.completedFuture(result);
        } catch (IOException e) {
            logger.error("Error processing resume asynchronously: {}", file.getOriginalFilename(), e);
            CompletableFuture<ResumeParseResult> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    @Override
    public List<CompletableFuture<ResumeParseResult>> parseResumesInParallel(List<MultipartFile> files) {
        logger.info("Processing {} resumes in parallel", files.size());
        
        List<CompletableFuture<ResumeParseResult>> futures = new ArrayList<>();
        
        for (MultipartFile file : files) {
            futures.add(parseResumeAsync(file));
        }
        
        return futures;
    }
    
    /**
     * Extract text from a file based on its type.
     * 
     * @param file The file to extract text from
     * @param fileType The type of the file
     * @return The extracted text
     * @throws IOException If there is an error reading the file
     */
    private String extractText(MultipartFile file, String fileType) throws IOException {
        switch (fileType.toLowerCase()) {
            case "pdf":
                return extractTextFromPdf(file.getInputStream());
            case "docx":
                return extractTextFromDocx(file.getInputStream());
            case "txt":
                return extractTextFromTxt(file.getInputStream());
            default:
                throw new IllegalArgumentException("Unsupported file type: " + fileType);
        }
    }
    
    /**
     * Extract text from a PDF file.
     * 
     * @param inputStream The input stream of the PDF file
     * @return The extracted text
     * @throws IOException If there is an error reading the file
     */
    private String extractTextFromPdf(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
    
    /**
     * Extract text from a DOCX file.
     * 
     * @param inputStream The input stream of the DOCX file
     * @return The extracted text
     * @throws IOException If there is an error reading the file
     */
    private String extractTextFromDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            return extractor.getText();
        }
    }
    
    /**
     * Extract text from a TXT file.
     * 
     * @param inputStream The input stream of the TXT file
     * @return The extracted text
     * @throws IOException If there is an error reading the file
     */
    private String extractTextFromTxt(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
    
    /**
     * Get the file type from a filename.
     * 
     * @param filename The filename to get the type from
     * @return The file type
     */
    private String getFileType(String filename) {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            throw new IllegalArgumentException("Filename does not have a valid extension: " + filename);
        }
        
        return filename.substring(lastDotIndex + 1);
    }
    
    /**
     * Extract structured information from resume text using AI.
     * 
     * @param resumeText The resume text to extract information from
     * @return A map of extracted information
     */
    private Map<String, String> extractInformation(String resumeText) {
        try {
            // Pre-process the resume text to remove problematic characters and normalize whitespace
            String cleanedText = resumeText
                    .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "") // Remove control chars except newlines and tabs
                    .replaceAll("\\p{Zs}+", " ") // Normalize spaces
                    .replaceAll("\\n\\s*\\n+", "\n\n") // Normalize multiple blank lines
                    .trim(); // Remove leading/trailing whitespace
                    
            // Limit text length if it's too long (LLMs have context limits)
            if (cleanedText.length() > 15000) {
                cleanedText = cleanedText.substring(0, 15000);
                logger.info("Resume text truncated to 15000 characters");
            }
            
            // Use the fluent API instead of PromptTemplate
            String response = chatClient.prompt()
                    .system("You are a resume parser. Extract information from the resume and format as JSON.")
                    .user("Extract the following information from this resume:\n" +
                          "1. Full name\n" +
                          "2. Email address\n" +
                          "3. Phone number\n\n" +
                          "Format your response as JSON:\n" +
                          "{\n" +
                          "  \"name\": \"...\",\n" +
                          "  \"email\": \"...\",\n" +
                          "  \"phoneNumber\": \"...\"\n" +
                          "}\n\n" +
                          "Resume text:\n" + cleanedText)
                    .call()
                    .content();
            
            // Extract JSON from the response (in case there's additional text)
            String jsonStr = extractJsonFromResponse(response);
            
            // Parse the JSON response
            JsonNode jsonNode = objectMapper.readTree(jsonStr);
            
            return Map.of(
                    "name", jsonNode.path("name").asText(""),
                    "email", jsonNode.path("email").asText(""),
                    "phoneNumber", jsonNode.path("phoneNumber").asText("")
            );
        } catch (Exception e) {
            logger.error("Error extracting information from resume: {}", e.getMessage(), e);
            
            // Try to extract partial information if possible
            try {
                // If we have a response but JSON parsing failed
                if (e.getMessage() != null && e.getMessage().contains("JSON")) {
                    logger.info("Attempting to extract information using regex patterns");
                    
                    // Extract information using regex patterns as a fallback
                    // Try multiple patterns for each field to increase chances of extraction
                    
                    // Name patterns - try multiple approaches
                    String name = extractPattern(resumeText, "(?i)(?:name|full name)[:\\s]*(.*?)(?:\\n|$)");
                    if (name == null) {
                        // Try to find a name at the beginning of the resume (first line)
                        String[] lines = resumeText.split("\\n");
                        if (lines.length > 0 && !lines[0].trim().isEmpty()) {
                            name = lines[0].trim();
                        }
                    }
                    if (name == null) {
                        // Try to find a name pattern at the beginning of the resume
                        name = extractPattern(resumeText, "^\\s*([A-Z][a-z]+(\\s+[A-Z][a-z]+){1,3})\\s*$");
                    }
                    
                    // Email patterns - try multiple approaches
                    String email = extractPattern(resumeText, "(?i)(?:e-?mail)[:\\s]*([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
                    if (email == null) {
                        // Try the specific format from the example resume
                        email = extractPattern(resumeText, "E-Mail:\\s*([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
                    }
                    if (email == null) {
                        // Try to find any email address in the text
                        email = extractPattern(resumeText, "([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
                    }
                    
                    // Phone patterns - try multiple approaches
                    String phone = extractPattern(resumeText, "(?i)(?:phone|mobile|cell|contact)[:\\s]*([0-9+\\s()-]{7,})");
                    if (phone == null) {
                        // Try the specific format from the example resume
                        phone = extractPattern(resumeText, "Mobile No:\\s*([0-9+\\s/()-]{7,})");
                    }
                    if (phone == null) {
                        // Try to find phone numbers with specific formats
                        phone = extractPattern(resumeText, "(\\+?[0-9]{1,3}[\\s-]?[0-9]{3,4}[\\s-]?[0-9]{3,4}[\\s-]?[0-9]{3,4})");
                    }
                    if (phone == null) {
                        // Try to find any sequence of digits that looks like a phone number
                        phone = extractPattern(resumeText, "([0-9]{3,4}[\\s-]?[0-9]{3,4}[\\s-]?[0-9]{3,4})");
                    }
                    
                    // Log what we found
                    logger.info("Extracted via regex - Name: {}, Email: {}, Phone: {}", 
                            name != null ? name : "Not found",
                            email != null ? email : "Not found",
                            phone != null ? phone : "Not found");
                    
                    if (name != null || email != null || phone != null) {
                        return Map.of(
                                "name", name != null ? name : "Unknown",
                                "email", email != null ? email : "unknown@example.com",
                                "phoneNumber", phone != null ? phone : "Unknown"
                        );
                    }
                }
            } catch (Exception ex) {
                logger.error("Error during fallback extraction: {}", ex.getMessage());
            }
            
            // Default fallback
            return Map.of(
                    "name", "Unknown",
                    "email", "unknown@example.com",
                    "phoneNumber", "Unknown"
            );
        }
    }
    
    /**
     * Extract JSON from a response that might contain additional text.
     * 
     * @param response The response from the AI
     * @return The extracted JSON string
     */
    private String extractJsonFromResponse(String response) {
        // Try to find JSON object in the response
        int startBrace = response.indexOf('{');
        int endBrace = response.lastIndexOf('}');
        
        if (startBrace >= 0 && endBrace > startBrace) {
            // Found JSON-like structure
            return response.substring(startBrace, endBrace + 1);
        }
        
        // If no JSON structure found, return the original response
        logger.warn("No JSON structure found in response: {}", response);
        return response;
    }
    
    /**
     * Extract information using regex pattern.
     * 
     * @param text The text to extract from
     * @param pattern The regex pattern to use
     * @return The extracted information or null if not found
     */
    private String extractPattern(String text, String pattern) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }
}
