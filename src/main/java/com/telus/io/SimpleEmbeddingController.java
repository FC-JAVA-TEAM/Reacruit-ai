package com.telus.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simple-embedding")
public class SimpleEmbeddingController {
    
    private final EmbeddingModel embeddingModel;
    
    public SimpleEmbeddingController(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }
    
    @PostMapping("/embed-text")
    public float[] embedText(@RequestBody String text) {
        // Return float[] directly - most efficient
        return embeddingModel.embed(text);
    }
    
    @PostMapping("/embed-text-as-list")
    public List<Float> embedTextAsList(@RequestBody String text) {
        // Convert float[] to List<Float> manually
        float[] embedding = embeddingModel.embed(text);
        List<Float> result = new ArrayList<>(embedding.length);
        for (float value : embedding) {
            result.add(value);
        }
        return result;
    }
    
    @PostMapping("/similarity")
    public double calculateSimilarity(@RequestBody SimilarityRequest request) {
        // Get float arrays directly
        float[] embedding1 = embeddingModel.embed(request.getText1());
        float[] embedding2 = embeddingModel.embed(request.getText2());
        
        // Calculate cosine similarity with float arrays
        return calculateCosineSimilarity(embedding1, embedding2);
    }
    
    // Optimized method for float arrays
    private double calculateCosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Embedding dimensions must match");
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}



//Request DTO
class SimilarityRequest {
 private String text1;
 private String text2;
public String getText1() {
	return text1;
}
public void setText1(String text1) {
	this.text1 = text1;
}
public String getText2() {
	return text2;
}
public void setText2(String text2) {
	this.text2 = text2;
}

}
